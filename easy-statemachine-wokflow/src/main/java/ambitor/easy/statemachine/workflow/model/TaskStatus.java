package ambitor.easy.statemachine.workflow.model;

public enum TaskStatus {
    /**
     * 待执行
     */
    open,
    /**
     * 执行中
     */
    running,
    /**
     * 执行失败
     */
    error,
    /**
     * 执行结束
     */
    close,
    /**
     * 重试后最终执行失败
     */
    terminal,
    /**
     * 添加此状态，用来让任务挂起不被扫描
     */
    suspend
}
