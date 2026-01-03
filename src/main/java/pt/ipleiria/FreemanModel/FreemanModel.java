package pt.ipleiria.FreemanModel;

import pt.ipleiria.common.KeystrokeEvent;
import pt.ipleiria.common.UserTypingData;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FreemanModel {

    // Pesos sugeridos pelo artigo (adaptados para as features disponíveis)
    // O artigo usa: IP (0.6), UA (0.53), Browser (0.27).
    // Como temos Biometria, damos-lhe um peso relevante.
    private static final Map<String, Double> FEATURE_WEIGHTS = new HashMap<>();

    static {
        FEATURE_WEIGHTS.put("PLATFORM_BROWSER", 0.0); // Equivalente a UA parcial
        FEATURE_WEIGHTS.put("RESOLUTION", 0.0);
        FEATURE_WEIGHTS.put("IS_MOBILE", 0.5);
        FEATURE_WEIGHTS.put("PWD_TOTAL_SPEED", 0.3);        // Biometria (Keystroke)
        FEATURE_WEIGHTS.put("PWD_AVG_SEEK", 0.6);        // Biometria (Keystroke)
        FEATURE_WEIGHTS.put("PWD_AVG_PRESS", 0.6);        // Biometria (Keystroke)
        FEATURE_WEIGHTS.put("USER_TOTAL_SPEED", 0.2);       // Biometria (Keystroke)
        FEATURE_WEIGHTS.put("USER_AVG_SEEK", 0.5);       // Biometria (Keystroke)
        FEATURE_WEIGHTS.put("USER_AVG_PRESS", 0.5);       // Biometria (Keystroke)
    }

    /**
     * Calcula o Risk Score baseado na fórmula de Freeman et al.
     * Score = Produto (P_Global / P_User)^Peso
     */
    public double calculateRiskScore(UserTypingData currentLogin, List<UserTypingData> userHistory) {
        double score = 1.0;

        // 1. Extrair Features do login atual (transformar JSON em Strings Categóricas)
        Map<String, String> currentFeatures = extractFeatures(currentLogin);

        for (Map.Entry<String, String> entry : currentFeatures.entrySet()) {
            String featureType = entry.getKey();   // Ex: "PLATFORM_BROWSER"
            String featureValue = entry.getValue(); // Ex: "Linux x86_64_blink"

            // 2. Calcular Probabilidade do Utilizador (Histórico Pessoal)
            double probUser = calculateUserProbability(featureType, featureValue, userHistory);

            // 3. Calcular Probabilidade Global (Histórico de Todos)
            // NOTA: Num sistema real, isto consulta uma DB/Cache global.
            double probGlobal = GlobalStatsManager.getInstance().getGlobalProbability(featureType, featureValue);

            // 4. Calcular Ratio (Raridade Relativa)
            // Se ProbUser é 0 (nunca visto), usamos um valor muito pequeno (suavização)
            if (probUser == 0) probUser = 0.0001;

            double ratio = probGlobal / probUser;

            // 5. Aplicar Pesos (Feature Weighting)
            double weight = FEATURE_WEIGHTS.getOrDefault(featureType, 0.5);

            // Fórmula final aplicada iterativamente: Score = Score * (Ratio ^ Peso)
            score *= Math.pow(ratio, weight);
        }

        return score;
    }

    /**
     * Converte os dados brutos do objeto Java em Features Categóricas (Strings).
     * Inclui discretização (Binning) para tempos.
     */
    public static Map<String, String> extractFeatures(UserTypingData data) {
        Map<String, String> features = new HashMap<>();

        // 1. Device Info (Mantém-se igual)
        if (data.deviceInfo != null) {
            String platform = data.deviceInfo.platform != null ? data.deviceInfo.platform : "unknown";
            String browser = data.deviceInfo.browser != null ? data.deviceInfo.browser : "unknown";
            features.put("PLATFORM_BROWSER", platform + "_" + browser);

            if (data.deviceInfo.screenDimensions != null) {
                features.put("RESOLUTION", data.deviceInfo.screenDimensions.resolution);
            }

            features.put("IS_MOBILE", data.deviceInfo.isMobile.toString());
        }

        // 2. Biometria (Username) - ATUALIZADO
        if (data.usernameData != null) {

            // A. Velocidade Total (Visão Macro)
            if (data.usernameData.totalTimeMs > 0) {
                long bucket = Math.round(data.usernameData.totalTimeMs / 250.0) * 250;
                features.put("USER_TOTAL_SPEED", String.valueOf(bucket));
            }

            // B. Análise Detalhada (Visão Micro - Seek & Press)
            if (data.usernameData.events != null && !data.usernameData.events.isEmpty()) {
                double sumPressTime = 0;
                double sumSeekTime = 0;
                int validEvents = 0;

                for (KeystrokeEvent event : data.usernameData.events) {
                    // Filtro de sanidade
                    if (event.pressTime < 5000 && event.seekTime < 5000) {
                        sumPressTime += event.pressTime;
                        sumSeekTime += event.seekTime;
                        validEvents++;
                    }
                }

                if (validEvents > 0) {
                    // Médias
                    double avgPress = sumPressTime / validEvents;
                    double avgSeek = sumSeekTime / validEvents;

                    // Binning (Igual à Password)
                    long seekBucket = Math.round(avgSeek / 25.0) * 25;
                    features.put("USER_AVG_SEEK", String.valueOf(seekBucket));

                    long pressBucket = Math.round(avgPress / 20.0) * 20;
                    features.put("USER_AVG_PRESS", String.valueOf(pressBucket));
                }
            }
        }

        // 3. Biometria (Password) - ONDE APLICAMOS A LÓGICA NOVA
        if (data.passwordData != null) {

            // A. Velocidade Total (Visão Macro)
            if (data.passwordData.totalTimeMs > 0) {
                long bucket = Math.round(data.passwordData.totalTimeMs / 250.0) * 250;
                features.put("PWD_TOTAL_SPEED", String.valueOf(bucket));
            }

            // B. Análise Detalhada dos Eventos (Visão Micro)
            if (data.passwordData.events != null && !data.passwordData.events.isEmpty()) {
                double sumPressTime = 0;
                double sumSeekTime = 0;
                int validEvents = 0;

                for (KeystrokeEvent event : data.passwordData.events) {
                    // Filtro de sanidade: ignorar pausas gigantes (ex: > 5 seg) que distorcem a média
                    if (event.pressTime < 5000 && event.seekTime < 5000) {
                        sumPressTime += event.pressTime;
                        sumSeekTime += event.seekTime;
                        validEvents++;
                    }
                }

                if (validEvents > 0) {
                    // Calcular Médias
                    double avgPress = sumPressTime / validEvents;
                    double avgSeek = sumSeekTime / validEvents;

                    // --- DISCRETIZAÇÃO (BINNING) ---
                    long seekBucket = Math.round(avgSeek / 25.0) * 25;
                    features.put("PWD_AVG_SEEK", String.valueOf(seekBucket));

                    // 2. Média de Press Time (Dwell Time)
                    long pressBucket = Math.round(avgPress / 20.0) * 20;

                    features.put("PWD_AVG_PRESS", String.valueOf(pressBucket));
                }
            }
        }

        return features;
    }

    /**
     * P(Feature | User) = (Contagem + 1) / (Total + Vocabulário)
     * Laplace Smoothing aplicado.
     */
    private double calculateUserProbability(String targetType, String targetValue, List<UserTypingData> history) {
        if (history.isEmpty()) return 0.0;

        long count = 0;
        for (UserTypingData historicalEntry : history) {
            Map<String, String> historicalFeatures = extractFeatures(historicalEntry);

            // Verifica se este registo histórico tem o mesmo valor para esta feature
            if (targetValue.equals(historicalFeatures.get(targetType))) {
                count++;
            }
        }

        // Suavização (Laplace) para evitar 0 absoluto
        double numerator = count + 1;
        double denominator = history.size() + 1.0; // 10 é um valor arbitrário de "Vocabulário" para estabilidade

        return numerator / denominator;
    }
}