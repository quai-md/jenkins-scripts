import com.google.gson.annotations.SerializedName

class FiberyQueryResponse_Result {
    @SerializedName(value = "fibery/public-id")
    String publicId;
    @SerializedName(value = "fibery/id")
    String id;
    @SerializedName(value = "workflow/state")
    FiberyQueryResponse_WorkflowState state;
}