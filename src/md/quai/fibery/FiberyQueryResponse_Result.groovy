import com.google.gson.annotations.SerializedName

@Grab('com.google.code.gson:gson:2.8.6')

class FiberyQueryResponse_Result {
    @SerializedName("fibery/public-id")
    String publicId;
    @SerializedName("fibery/id")
    String id;
    @SerializedName("workflow/state")
    FiberyQueryResponse_WorkflowState state;
}