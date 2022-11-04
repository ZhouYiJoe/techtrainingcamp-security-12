package com.catchyou.pojo;

import com.catchyou.constant.UserType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User {
    private String id;
    private String username;
    private String phoneNumber;
    private String password;
    private Date registerTime;
    private String registerIp;
    private String registerDeviceId;
    private Integer isActive;
    private UserType type;
}
