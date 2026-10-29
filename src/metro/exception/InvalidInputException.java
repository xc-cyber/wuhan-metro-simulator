package metro.exception;

/**
 * 输入不合法（空输入、非法数字、格式错误等）时抛出。
 * 继承 Exception，属于受检异常，调用者必须显式处理。
 */
public class InvalidInputException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
