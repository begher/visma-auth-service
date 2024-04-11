//package begh.vismaauthservice.access;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.sql.Timestamp;
//@Getter
//@Setter
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//@Entity
//@Table(name = "access")
//public class Access {
//    @Id
//    @GeneratedValue
//    private Integer id;
//    private String company;
//    @Column(name = "auth_code")
//    private String authCode;
//    @Column(name = "access_code")
//    private String accessCode;
//    @Column(name = "refresh_token")
//    private String refreshToken;
//    @Column(name = "created_at")
//    private Timestamp createdAt;
//    @Column(name = "activation_code")
//    private String activationCode;
//    private String auth;
//    @Column(name = "last_renewed_at")
//    private Timestamp lastRenewedAt;
//}