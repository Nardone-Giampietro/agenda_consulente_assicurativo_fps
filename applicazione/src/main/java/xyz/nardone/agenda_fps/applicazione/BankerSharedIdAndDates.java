package xyz.nardone.agenda_fps.applicazione;

/**
 * Singleton holder for the selected banker ID and date range.
 */
public class BankerSharedIdAndDates {
    private static final BankerSharedIdAndDates instance = new BankerSharedIdAndDates();

    private Integer id;
    private String startDate;
    private String endDate;

    private BankerSharedIdAndDates() {}

    public static BankerSharedIdAndDates getInstance() {
        return instance;
    }

    public Integer geId() {
        return id;
    }
    public String getStartDate() {
        return startDate;
    }
    public String getEndDate() {
        return endDate;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

}
