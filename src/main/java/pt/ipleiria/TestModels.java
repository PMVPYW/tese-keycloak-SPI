package pt.ipleiria;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import pt.ipleiria.FreemanModel.FreemanAuthenticator;
import pt.ipleiria.FreemanModel.FreemanModel;
import pt.ipleiria.FreemanModel.GlobalStatsManager;
import pt.ipleiria.OneClassSVM.BiometricRiskModel;
import pt.ipleiria.common.UserTypingData;

import java.io.InputStream;

import java.util.List;

public class TestModels {
    public static void main(String[] args) {
        List<UserTypingData> test_data;
        List<UserTypingData> training_data;
        List<UserTypingData> population_data;
        try (InputStream is = TestModels.class.getResourceAsStream("/data.json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            test_data =
                    mapper.readValue(
                            is,
                            new TypeReference<List<UserTypingData>>() {
                            }
                    );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        try (InputStream is = TestModels.class.getResourceAsStream("/trainData.json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            training_data =
                    mapper.readValue(
                            is,
                            new TypeReference<List<UserTypingData>>() {
                            }
                    );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        try (InputStream is = TestModels.class.getResourceAsStream("/population.json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            population_data =
                    mapper.readValue(
                            is,
                            new TypeReference<List<UserTypingData>>() {
                            }
                    );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
        training_data.removeIf(u -> u.deviceInfo != null && Boolean.TRUE.equals(u.deviceInfo.isMobile));
        System.out.println("Total registos treino: " + training_data.size());
        System.out.println("Total registos teste: " + test_data.size());

        for (UserTypingData data : population_data) {
            GlobalStatsManager.getInstance().addToStats(data);
        }
        for (UserTypingData data : training_data) {
            GlobalStatsManager.getInstance().addToStats(data);
        }


        //modelo de freeman
        int failedClassifications = 0;
        FreemanModel freemanModel = new FreemanModel();
        for (UserTypingData data : test_data) {
            double riskScore = freemanModel.calculateRiskScore(data, training_data);
            if (!(riskScore > FreemanAuthenticator.RISK_THRESHOLD)) {
                failedClassifications++;
            }
        }

        System.out.println("False acceptance rate (FAR) Freeman: " + (((double) failedClassifications) / test_data.size()) + "|failed classifications: " + failedClassifications);

        //ocsvm
        failedClassifications = 0;
        BiometricRiskModel ocsvm = new BiometricRiskModel();
        for (UserTypingData data : test_data) {
            double score = ocsvm.calculateRiskWithSVM(data, training_data);
            if (!(score < 0)) {
                failedClassifications++;
            }
        }

        System.out.println("False acceptance rate (FAR) OCSVM: " + ((double) failedClassifications / test_data.size()) + "|failed classifications: " + failedClassifications);

        //isolatonForest
        failedClassifications = 0;
        pt.ipleiria.IsolationFlorest.BiometricRiskModel isoForest = new pt.ipleiria.IsolationFlorest.BiometricRiskModel();
        for (UserTypingData data : test_data) {
            double score = isoForest.calculateRiskWithIsolationFLoresr(data, training_data);
            if (!(score > 0.6)) {
                failedClassifications++;
            }
        }

        System.out.println("False acceptance rate (FAR) iso forest: " + ((double) failedClassifications / test_data.size()) + "|failed classifications: " + failedClassifications);
    }
}
