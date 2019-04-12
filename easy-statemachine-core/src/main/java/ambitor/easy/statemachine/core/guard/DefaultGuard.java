package ambitor.easy.statemachine.core.guard;


import ambitor.easy.statemachine.core.exception.StateMachineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author Ambitor
 */
@Slf4j
public class DefaultGuard {
    public static <S, E> Guard<S, E> condition(String key, String expect) {
        return (s) -> {
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(expect)) {
                throw new StateMachineException("key or expect can not be blank...");
            }
            Object actualValue = s.getMessage().getHeaders().getHeader(key);
            if (actualValue == null) return false;
            return expect.equals(actualValue);
        };
    }
}
