import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    // Begin - Parametry symulacji
    protected static int numberOfProcessors = 50;
    protected static int numberOfTasks = 1000; // na procesor
    protected static int loggingFrequency = 5; // Co ile jednostek czasu zapisuję obciążenie procesorów.
    protected static int maxLoad = 90; // Próg maksymalnego obciążenia w %
    protected static int minLoad = 50; // Próg minimalnego obciążenia w %
    protected static int maxRequests = numberOfProcessors/2; // Maksymalna liczba zapytań do innych procesorów w strategii 1.
    protected static boolean showPerProcessorLoad = false; // Czy pokazywać średnie obciążenie każdego procesora
    protected static int taskGeneratedProbability = 35; // Szansa na wygenerowanie nowego zadania na procesorze w %

    // End - Parametry symulacji
    protected static ArrayList<Processor> allProcessors = new ArrayList<>();
    protected static int requestsCount;
    protected static int transfersCount;

    public static void main(String[] args) {
        allProcessors = Generator.generateProcessors(numberOfProcessors, numberOfTasks);
        run(1);
        allProcessors.forEach(Processor::reset);
        run(2);
        allProcessors.forEach(Processor::reset);
        run(3);

    }

    public static void run(int strategyNumber) { // numer strategii
        requestsCount = 0;
        transfersCount = 0;
        int timeToLog = loggingFrequency;

        while(!allTasksDone()) {

            //Na każdym procesorze jest szansa na wygenerowanie nowego zadania
            for (Processor processor : allProcessors) {
                if (randomProbability(taskGeneratedProbability)) {
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
            System.out.println("Procesor " + (i+1) + ": " + allProcessors.get(i).getAverageLoad() + "%");
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

    private static boolean randomProbability(int probability) {
        return ThreadLocalRandom.current().nextInt(100) < probability;
    }

    private static void transferTask(Processor from, Processor to) {
        to.runningTasks.add(from.getLatestTask());
        from.runningTasks.remove(from.getLatestTask());
        transfersCount++;
    }


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
                    transferTask(currentProcessor, randomProcessor);
                    break;
                }
            }
            // Jeżeli skończyły się próby i nie przeniosłem procesu to nic nie robię, wykona się on tam gdzie został utworzony.
        }
    }
    private static void strategy2(Processor currentProcessor) {
        // Jeżeli currentProcessor ma load powyżej maxload to odpytujemy losowe procesory czy mają poniżej maxload.
        // Jeśli tak to przesyłamy na taki procesor, jeśli odpytamy wszystkie bez sukcesu to nic nie robimy.

        if (currentProcessor.currentLoad() < maxLoad) return;
        ArrayList<Processor> askedProcessors = new ArrayList<>();
        askedProcessors.add(currentProcessor);

        while(askedProcessors.size()< numberOfProcessors) {
            // Losuje procesor którego zapytam
            Processor randomProcessor = allProcessors.get(ThreadLocalRandom.current().nextInt(allProcessors.size()));
            // Jeżeli już pytałem ten procesor to nic nie robię i losuje inny.
            if (!askedProcessors.contains(randomProcessor)) {
                // Zapamiętuje że wykonałem zapytanie do tego procesora
                requestsCount++;
                askedProcessors.add(randomProcessor);
                // Jeżeli procesor ma obciążenie poniżej progu to przenoszę zadanie które zostało teraz utworzone.
                if (randomProcessor.currentLoad() < maxLoad) {
                    transferTask(currentProcessor, randomProcessor);
                    break;
                }
            }
        }

    }
    private static void strategy3() {
        //Losujemy procesy, jeżeli jakiś ma obciążenie poniżej minimalnego progu, to odpytuje inne procesory.
        ArrayList<Processor> askedProcessors = new ArrayList<>();
        boolean foundProcessor = false;
        Processor askingProcessor = allProcessors.get(0); // tylko wartosc poczatkowa, jest nadpisywany losowaniem
        // Wykonuje, dopóki nie zapytałem wszystkich procesów lub znalazłem odpowiedni.
        while(askedProcessors.size()< numberOfProcessors) {
            askingProcessor = allProcessors.get(ThreadLocalRandom.current().nextInt(allProcessors.size()));
            if (!askedProcessors.contains(askingProcessor)) {
                askedProcessors.add(askingProcessor);
                requestsCount++;
                if (askingProcessor.currentLoad() < minLoad) {
                    foundProcessor = true;
                    break;
                }
            }
        }
        if (!foundProcessor) return; // Jeżeli nie ma procesora o obciążeniu mniejszym od minimum, to nic się nie dzieje.
        askedProcessors.clear();
        askedProcessors.add(askingProcessor);

        while(askedProcessors.size()< numberOfProcessors) {
            // Losuje procesor do zapytania
            Processor randomProcessor = allProcessors.get(ThreadLocalRandom.current().nextInt(allProcessors.size()));
            // Jeżeli już pytałem ten procesor, to nic nie robię i losuje inny.
            if (!askedProcessors.contains(randomProcessor)) {
                // Zapamiętuje, że wykonałem zapytanie do tego procesora
                requestsCount++;
                askedProcessors.add(randomProcessor);
                // Jeżeli procesor ma obciążenie powyżej max, to przejmujemy jego zadania, dopóki nie osiągniemy minimum.
                if (randomProcessor.currentLoad() > maxLoad) {
                    while(askingProcessor.currentLoad() < minLoad) {
                        transferTask(randomProcessor, askingProcessor);
                    }
                    break;
                }
            }
        }
    }


}