import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Generator {

    private static ArrayList<Task> generateTasks(int n) {
        ArrayList<Task> tasks = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int load = ThreadLocalRandom.current().nextInt(1, 10);
            int time = ThreadLocalRandom.current().nextInt(1, 100);
            tasks.add(new Task(load, time));
        }
        return tasks;
    }
    public static ArrayList<Processor> generateProcessors(int processors, int tasks) {
        ArrayList<Processor> processorsList = new ArrayList<>();
        for (int i = 0; i < processors; i++) {
            processorsList.add(new Processor(generateTasks(tasks)));
        }
        return processorsList;
    }

}
