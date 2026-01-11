package pt.ipleiria.OneClassSVM;

import pt.ipleiria.common.KeystrokeEvent;
import pt.ipleiria.common.UserTypingData;
import smile.base.svm.KernelMachine;
import smile.base.svm.OCSVM;
import smile.math.kernel.GaussianKernel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiometricRiskModel {

    private double[] calculateStats(List<KeystrokeEvent> events) {
        double sumPress = 0;
        double sumSeek = 0;
        int count = 0;

        for (KeystrokeEvent e : events) {
            // Filtro básico de sanidade
            if (e.pressTime < 5000 && e.seekTime < 5000) {
                sumPress += e.pressTime;
                sumSeek += e.seekTime;
                count++;
            }
        }

        if (count == 0) return new double[]{0, 0};

        return new double[] {
                sumPress / count, // Média Press
                sumSeek / count   // Média Seek
        };
    }

    // --- PASSO 1: EXTRAÇÃO (O seu método atual, ligeiramente limpo) ---
    // Recebe APENAS um registo. Transforma Objeto -> Array Bruto.
    private double[] extractRawFeatures(UserTypingData data) {
        double[] vector = new double[6];

        // ... (A sua lógica de Password e Username aqui) ...
        // Exemplo simplificado:
        if (data.passwordData != null && data.passwordData.events != null) {
            double[] stats = calculateStats(data.passwordData.events);
            vector[0] = stats[0]; // Press
            vector[1] = stats[1]; // Seek
            vector[2] = (double) data.passwordData.totalTimeMs;
        }
        if (data.usernameData != null && data.usernameData.events != null) {
            double[] stats = calculateStats(data.usernameData.events);
            vector[3] = stats[0]; // Press
            vector[4] = stats[1]; // Seek
            vector[5] = (double) data.usernameData.totalTimeMs;
        }

        return vector;
    }

    // --- PASSO 2: CÁLCULO DE RISCO (Onde "Todos os Dados" entram) ---
    public double calculateRiskWithSVM(UserTypingData currentLogin, List<UserTypingData> history) {

        // 1. Converter TODO o histórico em vetores brutos
        List<double[]> rawHistoryVectors = new ArrayList<>();
        for (UserTypingData historicEntry : history) {
            rawHistoryVectors.add(extractRawFeatures(historicEntry));
        }

        // 2. Extrair vetor bruto do login ATUAL
        double[] currentRawVector = extractRawFeatures(currentLogin);

        // 3. Calcular Min/Max baseados no HISTÓRICO (O contexto necessário)
        double[][] minMax = calculateMinMax(rawHistoryVectors);
        double[] min = minMax[0];
        double[] max = minMax[1];

        // 4. Normalizar o Treino (Histórico)
        double[][] normalizedTrainingData = rawHistoryVectors.stream()
                .map(v -> normalize(v, min, max))
                .toArray(double[][]::new);

        // 5. Normalizar o Login Atual (Usando a régua do histórico!)
        double[] normalizedCurrentVector = normalize(currentRawVector, min, max);

        // 6. Treinar SVM e Predizer
        // (Aqui entra o código do SMILE que vimos antes)
        GaussianKernel kernel = new GaussianKernel(60.0);
        OCSVM<double[]> ocsvm = new OCSVM<double[]>(kernel, 0.05, 1E-3);
        KernelMachine<double[]> classifier = ocsvm.fit(normalizedTrainingData);

        return classifier.score(normalizedCurrentVector);
    }

    // --- AUXILIAR: Normalização Min-Max ---
    private double[] normalize(double[] vector, double[] min, double[] max) {
        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            double range = max[i] - min[i];
            if (range == 0) {
                normalized[i] = 0.5; // Evitar divisão por zero se valor for sempre igual
            } else {
                normalized[i] = (vector[i] - min[i]) / range;
            }
        }
        return normalized;
    }

    // --- AUXILIAR: Descobrir limites do utilizador ---
    private double[][] calculateMinMax(List<double[]> vectors) {
        int dims = vectors.get(0).length;
        double[] min = new double[dims];
        double[] max = new double[dims];

        // Inicializar com valores extremos
        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);

        for (double[] v : vectors) {
            for (int i = 0; i < dims; i++) {
                if (v[i] < min[i]) min[i] = v[i];
                if (v[i] > max[i]) max[i] = v[i];
            }
        }
        return new double[][]{min, max};
    }
}