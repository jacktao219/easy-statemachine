package ambitor.easy.statemachine.sf.service;

import ambitor.easy.statemachine.model.StateMachineLog;
import ambitor.easy.statemachine.service.StateMachineLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by Ambitor on 2019/4/2
 */
@Slf4j
@Component
public class StateMachineLogServiceImpl implements StateMachineLogService {
    @Override
    public int insertSelective(StateMachineLog record) {
        log.info("插入状态机日志 StateMachineCode ->{}", record.getMachineCode());
        return 1;
    }
}
