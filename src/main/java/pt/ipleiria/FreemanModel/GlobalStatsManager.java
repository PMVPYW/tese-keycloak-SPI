package pt.ipleiria.FreemanModel;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;
import pt.ipleiria.common.KeystrokeHistoryEntryDTO;
import pt.ipleiria.common.UserTypingData;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class GlobalStatsManager {

    private static final Logger logger = Logger.getLogger(GlobalStatsManager.class);

    // Instância única (Singleton) para manter os dados em memória RAM
    private static final GlobalStatsManager INSTANCE = new GlobalStatsManager();

    // A "Hash Table" mencionada no artigo.
    // Estrutura: Tipo de Feature -> Valor da Feature -> Contagem
    // Exemplo: "PLATFORM_BROWSER" -> "Linux_Chrome" -> 150
    private final Map<String, Map<String, AtomicLong>> globalCounts = new ConcurrentHashMap<>();

    // Total de eventos processados (necessário para calcular probabilidades)
    private final AtomicLong totalGlobalEvents = new AtomicLong(0);

    // Flag para evitar recarregar a base de dados repetidamente
    private volatile boolean isInitialized = false;

    private GlobalStatsManager() {}

    public static GlobalStatsManager getInstance() {
        return INSTANCE;
    }

    /**
     * --- A FUNÇÃO DE CONSULTA ---
     * Percorre todos os utilizadores do Realm, extrai o histórico de typing,
     * e preenche a Hash Table em memória.
     */
    public synchronized void loadGlobalStats(KeycloakSession session, RealmModel realm, boolean isMobile) {
        // Se já carregámos os dados, não fazemos nada (para não bloquear o login)
        if (isInitialized) return;

        logger.info("FreemanModel: A iniciar leitura total da BD para criar Hash Table Global...");
        long start = System.currentTimeMillis();

        // 1. Limpar dados antigos (caso seja um reload forçado)
        globalCounts.clear();
        totalGlobalEvents.set(0);

        // 2. Stream otimizada do Keycloak para ler utilizadores em batch
        Stream<UserModel> usersStream = session.users().searchForUserStream(realm, Collections.emptyMap(), 0, Integer.MAX_VALUE);

        // 3. Processar cada utilizador
        usersStream.forEach(user -> {
            // Obtém o atributo onde guardas o JSON
            List<String> history = user.getAttributeStream("keystroke_history_decoded").toList();

            for (String jsonEntry : history) {
                try {
                    // Converter JSON string para Objeto Java
                    KeystrokeHistoryEntryDTO dto = JsonSerialization.readValue(jsonEntry, KeystrokeHistoryEntryDTO.class);
                    UserTypingData data = dto.toUserTypingData();

                    // Adicionar este registo às estatísticas globais
                    if (data.validated && data.deviceInfo.isMobile == isMobile) {
                        addToStats(data);
                    }

                } catch (IOException e) {
                    // Ignora registos corrompidos, mas faz log
                    logger.warn("Erro ao processar histórico do user: " + user.getUsername());
                }
            }
        });

        isInitialized = true;
        long duration = System.currentTimeMillis() - start;
        logger.infof("FreemanModel: Hash Table carregada em %d ms. Total de eventos analisados: %d",
                duration, totalGlobalEvents.get());
    }

    /**
     * Adiciona um registo à memória (usado no load inicial e em novos logins)
     */
    public void addToStats(UserTypingData data) {
        totalGlobalEvents.incrementAndGet();

        // Usa o método estático do FreemanModel para garantir que as features
        // são extraídas exatamente da mesma maneira (ex: arredondamento de tempos)
        Map<String, String> features = FreemanModel.extractFeatures(data);

        for (Map.Entry<String, String> entry : features.entrySet()) {
            String featureType = entry.getKey();   // Ex: "PWD_SPEED"
            String featureValue = entry.getValue(); // Ex: "5500"

            // Inicializa os mapas se não existirem (Thread-safe)
            globalCounts.putIfAbsent(featureType, new ConcurrentHashMap<>());
            globalCounts.get(featureType).putIfAbsent(featureValue, new AtomicLong(0));

            // Incrementa o contador atomicamente
            globalCounts.get(featureType).get(featureValue).incrementAndGet();
        }
    }

    /**
     * Obtém a probabilidade global de uma feature (com Laplace Smoothing)
     * Fórmula baseada no artigo de Freeman et al.
     */
    public double getGlobalProbability(String featureType, String featureValue) {
        // Se a feature nunca foi vista globalmente, assumimos probabilidade mínima
        if (!globalCounts.containsKey(featureType)) {
            return 0.00001;
        }

        Map<String, AtomicLong> valueCounts = globalCounts.get(featureType);

        long count = valueCounts.containsKey(featureValue) ? valueCounts.get(featureValue).get() : 0;
        long total = totalGlobalEvents.get();

        // Laplace Smoothing: (count + 1) / (total + vocabulario)
        // Evita divisão por zero e probabilidades nulas
        return (double) (count + 1) / (total + 100);
    }

    // Método auxiliar para forçar recarregamento (ex: via Admin API)
    public synchronized void forceReload() {
        this.isInitialized = false;
    }
}