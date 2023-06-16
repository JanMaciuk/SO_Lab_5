import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    // Begin - Parametry symulacji
    protected static int numberOfProcessors = 50;
    protected static int numberOfTasks = 1000; // na procesor
    protected static int loggingFrequency = 5; // Co ile jednostek czasu zapisuję obciążenie procesorów.
    protected static int maxLoad = 90; // Próg maksymalnego obciążenia w %
    protected static int maxRequests = numberOfProcessors/2; // Maksymalna liczba zapytań do innych procesorów w strategii 1.
    protected static boolean showPerProcessorLoad = true; // Czy pokazywać średnie obciążenie każdego procesora

    // End - Parametry symulacji
    protected static ArrayList<Processor> allProcessors = new ArrayList<>();
    protected static int requestsCount;
    protected static int transfersCount;

    public static void main(String[] args) {
        allProcessors = Generator.generateProcessors(numberOfProcessors, numberOfTasks);
        run(1);
        allProcessors.forEach(Processor::reset);

    }

    public static void run(int strategyNumber) { // numer strategii
        requestsCount = 0;
        transfersCount = 0;
        int timeToLog = loggingFrequency;

        while(!allTasksDone()) {

            //Na każdym procesorze jest szansa na wygenerowanie nowego zadania
            for (Processor processor : allProcessors) {
                if (ThreadLocalRandom.current().nextBoolean() && ThreadLocalRandom.current().nextBoolean() && ThreadLocalRandom.current().nextBoolean()) {
                    // Procesor aktywuje zadanie (jeżeli zostały jeszcze jakieś zadania oczekujące)
                    if (!processor.activateTask()) continue;
                    // Jeżeli nie udało się aktywować zadania bo lista jest pusta to nie wykonujemy strategii.
                    // Wykonuję odpowiednie akcje zależnie od tego która strategia jest implementowana.
                    if (strategyNumber == 1) strategy1(processor);
                    else strategy2(processor); // Strategia 2 i 3 mają takie samo działanie podczas aktywacji zadania
                }
            }

            // Upływa jedna jednostka czasu, każdy procesor wykonuje swoje zadania
            allProcessors.forEach(Processor::doActiveTasks);

            // Zapisuje obecne obciążenie procesorów raz na ileś jednostek czasu
            if (timeToLog == 0) {
                allProcessors.forEach(Processor::logLoad);
                timeToLog = loggingFrequency;
            } else {
                timeToLog--;
            }

            // Dobry samarytanin ze strategii 3
            if (strategyNumber == 3) { strategy3(); }

        }

        // Koniec symulacji, zwracam wyniki:
        System.out.println("\n---------------------------------------------");
        System.out.println("Wyniki symulacji dla strategii nr. " + strategyNumber + ":");
        int averageLoad = 0;
        for (Processor processor : allProcessors) { averageLoad += processor.getAverageLoad(); }
        averageLoad /= allProcessors.size();
        System.out.println("Średnie obciążenie wszystkich procesorów: " + averageLoad + "%");

        //Odchylenie standardowe
        double standardDeviation = 0.0;
        for (Processor process : allProcessors) {
            standardDeviation += Math.pow(process.getAverageLoad() - averageLoad, 2);
        }
        standardDeviation = Math.sqrt(standardDeviation / numberOfProcessors);
        System.out.println("Odchylenie standardowe: " + (float)standardDeviation);


        System.out.println("Liczba zapytań do innych procesorów: " + requestsCount);
        System.out.println("Liczba przeniesień pomiędzy procesorami: " + transfersCount);
        if (!showPerProcessorLoad) return;
        System.out.println("Obciążenie każdego procesora:");
        for (int i = 0; i < allProcessors.size(); i++) {
            System.out.println("Procesor " + i+1 + ": " + allProcessors.get(i).getAverageLoad() + "%");
        }


    }
    private static boolean allTasksDone() {
        for (Processor processor : allProcessors) { // Optymalny sposób bo najczęściej zwróci false po pierwszym porównaniu.
            if (!processor.waitingTasks.isEmpty() || !processor.runningTasks.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void transferTask(Processor from, Processor to, Task task) {
        from.runningTasks.remove(task);
        to.runningTasks.add(task);
        transfersCount++;
    }


    //TODO: implementacja strategii
    private static void strategy1(Processor currentProcessor) {
        // Zapytaj losowy procesor czy ma obciążenie poniżej progu maxLoad, jeśli tak to prześlij mu request.
        // Nie pytamy wiele razy tego samego procesu.
        // Pytamy maksymalnie maxRequests razy.
        int remainingRequestAttempts = maxRequests;
        ArrayList<Processor> askedProcessors = new ArrayList<>();
        askedProcessors.add(currentProcessor);

        while(remainingRequestAttempts > 0) {
            // Losuje procesor którego zapytam
            Processor randomProcessor = allProcessors.get(ThreadLocalRandom.current().nextInt(allProcessors.size()));
            // Jeżeli już pytałem ten procesor to nic nie robię i losuje inny.
            if (!askedProcessors.contains(randomProcessor)) {
                // Zapamiętuje że wykonałem zapytanie do tego procesora
                requestsCount++;
                askedProcessors.add(randomProcessor);
                remainingRequestAttempts--;
                // Jeżeli procesor ma obciążenie poniżej progu to przenoszę zadanie które zostało teraz utworzone.
                if (randomProcessor.currentLoad() < maxLoad) {
                    transferTask(currentProcessor, randomProcessor, currentProcessor.getLatestTask());
                    break;
                }
            }
            // Jeżeli skończyły się próby i nie przeniosłem procesu to nic nie robię, wykona się on tam gdzie został utworzony.
        }
    }
    private static void strategy2(Processor currentProcessor) {}
    private static void strategy3() {
        //Losujemy procesy, jeżeli jakiś ma obciążenie poniżej minimalnego progu to odpytuje inne procesory.
    }


}