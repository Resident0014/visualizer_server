public class Response {
    private String status;
    private Object data;

    public Response(String status, Object data) {
        this.status = status;
        this.data = data;
    }
}
