package md.quai.fibery

import com.nu.art.core.tools.StreamTools
import com.nu.art.http.Transaction_JSON
import com.nu.art.http.consts.HttpMethod
import groovy.json.JsonSlurper

import java.util.List

public class FiberyTransaction extends Transaction_JSON {

    //######################## Params ########################

    private final String token;

    //######################## Init ########################

    public FiberyTransaction(String token) {
        this.token = token;
    }

    //######################## Functionality ########################

    public List<Object> queryTasks(String[] taskPublicIds) {
        def queryBody = [[
                                 command: "fibery.entity/query",
                                 args   : [
                                         query : [
                                                 "q/from"  : "Main/Task",
                                                 "q/where" : ["q/in", "fibery/public-id", "\$publicIds"],
                                                 "q/limit" : "q/no-limit",
                                                 "q/select": ["fibery/id", "fibery/public-id", "Main/Name", ["workflow/state": ["enum/name", "fibery/id"]]]
                                         ],
                                         params: [
                                                 $publicIds: taskPublicIds
                                         ]
                                 ]
                         ]]
        String URL = "https://quai.fibery.io/api/commands";
        def stream = createRequest()
                .setMethod(HttpMethod.Post)
                .setUrl(URL)
                .addHeader("Authorization", "Token " + token)
                .setBody(gson.toJson(queryBody))
                .executeSync()

        def responseAsString = StreamTools.readFullyAsString(stream);
        def data = new JsonSlurper().parseText(responseAsString);
        List<Object> tasks = new ArrayList<Object>()

        data.each { query ->
            query.result.each { task ->
                tasks.add(task)
            }
        }

        return tasks
    }

    public void promoteTasks(Closure<String> stateIdResolver, List<Object> tasks) {
        def body = [];
        String URL = "https://quai.fibery.io/api/commands";

        tasks.each { task ->
            String stateId = stateIdResolver.call(task)
            body.add(generateTaskPromotionQuery(task['fibery/id'], stateId))
        }

        def updateStream = createRequest()
                .setMethod(HttpMethod.Post)
                .setUrl(URL)
                .addHeader("Authorization", "Token " + token)
                .setBody(gson.toJson(body))
                .executeSync()

//        def updateResponseAsString = StreamTools.readFullyAsString(updateStream);
//        this.logInfo(updateResponseAsString);
    }

    private LinkedHashMap<String, Object> generateTaskPromotionQuery(String taskId, String stateId) {
        return [
                command: "fibery.entity/update",
                args   : [
                        "type"  : "Main/Task",
                        "entity": [
                                "fibery/id"     : taskId,
                                "workflow/state": ["fibery/id": stateId]
                        ]
                ]
        ]
    }
}
