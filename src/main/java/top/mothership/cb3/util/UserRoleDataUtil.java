package top.mothership.cb3.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.mothership.cb3.pojo.domain.UserRoleEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class UserRoleDataUtil {

    @Autowired
    private ObjectMapper objectMapper;

    public List<String> sortRoles(String role) {
        List<String> roles = Arrays.asList(role.split(","));
        //此处自定义实现排序方法
        //dev>分群>主群>比赛
        roles.sort((o1, o2) -> {
//            mp5s优先级得低于mp5和各个分部，考虑到比赛选手刷超了超过mp4的，也得低于mp4
            if (o1.contains("mp5s") && (
                    o2.equals("mp5") || o2.equals("mp4")
                            || o2.equals("mp5mc") || o2.equals("mp5chart"))) {
                return -1;
            }
            if (o2.contains("mp5s") && (
                    o1.equals("mp5") || o1.equals("mp4")
                            || o1.equals("mp5mc") || o1.equals("mp5chart"))) {
                return 1;
            }
//            //比赛期间mp5s优先级比mp5高，只比mc和chart低
//            if (o1.contains("mp5s") && (o2.equals("mp5mc") || o2.equals("mp5chart"))) {
//                return -1;
//            }
            //mp4s<mp4
            //2018-1-29 15:30:59 兼容mp3
            if (o1.contains("mp4s") && o2.equals("mp4") || o2.equals("mp3")) {
                return -1;
            }
            if (o2.contains("mp4s") && o1.equals("mp4") || o1.equals("mp3")) {
                return 1;
            }
            //dev大于一切
            if (o1.equals("dev")) {
                return 1;
            }
            if (o2.equals("dev")) {
                return -1;
            }
            return o1.compareTo(o2);
        });
        Collections.reverse(roles);
        return roles;
    }

    public UserRoleEntity addRole(String role, UserRoleEntity user) {
        String newRole;
        //如果当前的用户组是creep，就直接改成现有的组
        if ("creep".equals(user.getRole())) {
            newRole = role;
        } else {
            //当用户不在想要添加的用户组的时候才添加 2017-11-27 20:45:20
            if (!Arrays.asList(user.getRole().split(",")).contains(role)) {
                newRole = user.getRole() + "," + role;
            } else {
                newRole = user.getRole();
            }

        }
        user.setRole(newRole);
        return user;
    }

    public UserRoleEntity delRole(String role, UserRoleEntity user) {
        //拿到原先的user，把role去掉
        String newRole;
        //这里如果不把Arrays.asList传入构造函数，而是直接使用会有个Unsupported异常
        //因为Arrays.asList做出的List是不可变的
        List<String> roles = new ArrayList<>(Arrays.asList(user.getRole().split(",")));
        //2017-11-27 21:04:36 增强健壮性，只有在含有这个role的时候才进行移除
        roles.remove(role);
        if(user.getMainRole().equals(role)){
            user.setMainRole("creep");
        }
        if ("All".equals(role) || roles.size() == 0) {
            newRole = "creep";
            user.setMainRole("creep");
        } else {
            //转换为字符串，此处得去除空格（懒得遍历+拼接了）
            newRole = roles.toString().replace(" ", "").
                    substring(1, roles.toString().replace(" ", "").indexOf("]"));
        }

        user.setRole(newRole);
        return user;
    }

    public UserRoleEntity renameUser(UserRoleEntity user, String newName) throws JsonProcessingException {
        //如果检测到用户改名，取出数据库中的现用名加入到曾用名，并且更新现用名和曾用名
        List<String> legacyUname = objectMapper.readValue(user.getLegacyUname(), new TypeReference<>() {
        });
        if (user.getCurrentUname() != null) {
            legacyUname.add(user.getCurrentUname());
        }
        user.setLegacyUname(objectMapper.writeValueAsString(legacyUname));
        user.setCurrentUname(newName);

        return user;
    }
}
