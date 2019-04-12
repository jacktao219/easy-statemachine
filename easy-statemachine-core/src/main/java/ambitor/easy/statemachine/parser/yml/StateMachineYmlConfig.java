package ambitor.easy.statemachine.parser.yml;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 状态机Yml配置
 * Created by Ambitor on 2019/4/9
 * @author Ambitor
 */
@Getter
@Setter
public class StateMachineYmlConfig {
    private String name;
    private List<String> interceptor;
    private StateEntry states;
    private List<String> events;
    private List<TransitionEntry> transitions;

    @Getter
    @Setter
    public static class StateEntry {
        private String init;
        private List<String> suspend;
        private List<String> end;
        private List<String> other;
    }

    @Getter
    @Setter
    public static class TransitionEntry {
        private String type;
        private String source;
        private String target;
        private String event;
        private String action;
        private String errorAction;
        private ChoiceTransitionVO first;
        private ChoiceTransitionVO then;
        private ChoiceTransitionVO last;
    }

    @Getter
    @Setter
    public static class ChoiceTransitionVO {
        private String status;
        private String equals;
        private String target;

        @Override
        public String toString() {
            return "ChoiceTransitionVO{" +
                    "status='" + status + '\'' +
                    ", equals='" + equals + '\'' +
                    ", target='" + target + '\'' +
                    '}';
        }
    }
}
