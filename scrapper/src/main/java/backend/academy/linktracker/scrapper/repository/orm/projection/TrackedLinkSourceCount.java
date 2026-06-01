package backend.academy.linktracker.scrapper.repository.orm.projection;

public interface TrackedLinkSourceCount {
    String getTrackedSource();

    Long getTotal();
}
