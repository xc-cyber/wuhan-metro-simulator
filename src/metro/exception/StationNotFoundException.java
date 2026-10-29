package metro.exception;

/**
 * 站点不存在或不可达时抛出。
 * 继承 RuntimeException，属于非受检异常，由上层统一捕获并为用户给出友好提示。
 */
public class StationNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StationNotFoundException(String message) {
        super(message);
    }

    public StationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
