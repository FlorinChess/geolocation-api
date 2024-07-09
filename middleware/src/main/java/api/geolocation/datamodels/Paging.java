package api.geolocation.datamodels;

import lombok.Data;

@Data
public class Paging {
    int skip;
    int take;
    int total;

    public Paging(int skip, int take, int total) {
        this.skip = skip;
        this.take = take;
        this.total = total;
    }
}
