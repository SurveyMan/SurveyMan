package interstitial;

public interface ITask {

    public String getTaskId();
    public Record getRecord();
    public void setRecord(Record record);
    @Override
    public boolean equals(Object o);
    @Override
    public int hashCode();

}
