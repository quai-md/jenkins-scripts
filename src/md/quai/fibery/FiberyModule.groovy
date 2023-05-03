package md.quai.fibery

import com.nu.art.modular.core.Module
import md.quai.fibery.FiberyTransaction
import com.nu.art.pipeline.modules.SlackModule
import com.nu.art.pipeline.exceptions.BadImplementationException

public class FiberyModule extends Module {
    private String token;
    public String env;
    def envProjects = [:]

    public void declareConfig(String env, FiberyEnvConfig config) {
        envProjects[env] = config;
    }

    @Override
    protected void init() {}

    public void setToken(String token) {
        this.token = token;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    private List<Object> tasksToBePromoted(List<Object> tasks) {
        List<Object> toRet = new ArrayList<Object>()
        tasks.each { task ->
            if (this.envProjects[this.env].shouldPromote.call(task))
                toRet.add(task)
        }
        return toRet
    }

    private List<Object> validateTasks(List<Object> tasks) {
        List<Object> nonPassingTasks = new ArrayList<Object>()
        tasks.each { task ->
            if (!this.envProjects[this.env].validateTask.call(task))
                nonPassingTasks.add(task)
        }
        return nonPassingTasks
    }

    public void promoteTasks(String[] taskPublicIds) {
        //Get tasks
        FiberyTransaction transaction = new FiberyTransaction(token);

        List<Object> tasks = transaction.queryTasks(taskPublicIds);
        tasks = this.tasksToBePromoted(tasks)

        //If no tasks to promote
        if (tasks.size() == 0)
            return

        List<Object> nonPassingTasks = this.validateTasks(tasks)

        //If any invalid tasks
        if (nonPassingTasks.size() > 0) {
            String message = "Task Promotion - Failed:\n"
            String error = "Failed promoting tasks:\n"

            nonPassingTasks.each { task ->
                message += this.generateTaskSlackMessage(task, false)
                error += "${task["fibery/public-id"]}\n"
            }

            getModule(SlackModule.class).notify(message, "#FF0000", "__web-lifecycle")
            throw new BadImplementationException(error);
        }

        //All tasks valid, promote
        transaction.promoteTasks(this.envProjects[this.env].resolveTaskState, tasks)
        String message = ":typingcat: *Task Promotion - Success:* :greatsuccess:\n"
        Boolean showTitle = true;

        //Print tasks to slack in groups of 10
        tasks.eachWithIndex { task, index ->
            if (index % 10 == 0 && index != 0) {
                getModule(SlackModule.class).notify(message, "#00FF00", "__web-lifecycle", showTitle)
                message = "";
                showTitle = false;
            }
            message += this.generateTaskSlackMessage(task, true)
        }

        getModule(SlackModule.class).notify(message, "#00FF00", "__web-lifecycle", showTitle)
    }

    private String generateTaskSlackMessage(Object task, Boolean success) {
        String stateId = this.envProjects[this.env].resolveTaskState.call(task)
        String stateName = this.envProjects[this.env].resolveStateName.call(stateId)
        String message = "- <https://quai.fibery.io/Main/Task/${task["fibery/public-id"]}|${task["fibery/public-id"]}>"
        if (success)
            message += " *[${stateName}]*"
        message += " - ${task["Main/Name"]}\n"
        return message
    }
}
