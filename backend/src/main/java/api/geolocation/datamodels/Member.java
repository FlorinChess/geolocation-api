package api.geolocation.datamodels;

import lombok.Data;

@Data
public class Member {
    private String type;
    private Long ref;
    private String role;

    @Override
    public String toString() {
        return "Member{" +
                "type='" + type + '\'' +
                ", ref=" + ref +
                ", role='" + role + '\'' +
                '}';
    }
}
