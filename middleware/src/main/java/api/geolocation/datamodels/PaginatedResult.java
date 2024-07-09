package api.geolocation.datamodels;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedResult<T> {
    List<T> entries;
    Paging paging;

    public PaginatedResult(Paging paging) {
        this.paging = paging;
    }
}
