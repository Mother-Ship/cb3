package top.mothership.cb3.mapper;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;
import top.mothership.cb3.pojo.domain.UserRoleEntity;

import java.util.List;


@Mapper
@Repository
public interface UserDAO {
    /**
     * Gets user.
     *
     * @param qq     the qq
     * @param userId the user id
     * @return the user
     */
    @Select("<script>" +
            "SELECT * FROM `userrole` " +
            "<choose>" +
            "<when test=\"qq != null\">" +
            "WHERE `qq` = #{qq}" +
            "</when>" +
            "<when test=\"userId != null\">" +
            "WHERE`user_id` = #{userId}" +
            "</when>" +
            "</choose>" +
            "</script>")
    //只能传一个，不能同时处理两个
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    UserRoleEntity getUser(@Param("qq") Long qq, @Param("userId") Integer userId);

    /**
     * List user id by role list.
     *
     * @param role the role
     * @return the list
     */
//加入分隔符处理，在中间的，开头的，结尾的，只有这一个用户组的
    @Select("<script>"
            + "SELECT `user_id` FROM `userrole` "
            + "<where>"
            + "<if test=\"role != null\">"
            + "(`role` LIKE CONCAT('%,',#{role},',%') "
            + "OR `role` LIKE CONCAT(#{role},',%') "
            + "OR `role` = #{role} "
            + "OR `role` LIKE CONCAT('%,',#{role})) </if>"
            + "<if test=\"unbanned = true\">"
            + "AND `is_banned` = '0' </if> " +
            "</where>"
            + "</script>")
    List<Integer> listUserIdByRole(@Param("role") String role, @Param("unbanned") Boolean unbanned);

    /**
     * List user id by uname list.
     * 改为Gson序列化，只需考虑在中间的问题，同时加入分隔符
     * 2018-3-12 16:26:59改为模糊查询
     * 2018-3-16 13:29:44没必要用动态sql吧？试试改为多个字段查询
     * 2018-3-21 12:13:29直接用OR字段搜user_id=用户名时会返回几百个结果，搜索了一下改为现在的查询
     *
     * @param keyword 搜索关键字
     * @return the list
     */
    @Select("SELECT * FROM `userrole` "
            + "WHERE concat( `user_id` ,',', `qq` ,',', `legacy_uname`,',', `current_uname`) LIKE CONCAT('%',#{keyword},'%')")
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    List<UserRoleEntity> searchUser(@Param("keyword") String keyword);

    @Select("SELECT * FROM `userrole` "
            + "WHERE `is_banned` =1 ")
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    List<UserRoleEntity> listBannedUser();

    /**
     * Gets repeat star.
     * 去掉100%复读的
     *
     * @return the repeat star
     */
    @Results(
            {
                    //手动绑定这个字段
                    @Result(column = "is_banned", property = "banned")
            })
    @Select("SELECT * FROM `userrole` WHERE `speaking_count` >10 order by `repeat_count`/`speaking_count` desc limit 1")
    UserRoleEntity getRepeatStar();

    /**
     * Update user integer.
     * 由于采用动态SQL，QQ只能是0不能是null
     *
     * @param user the user
     * @return the integer
     */

    @Update("<script>" + "update `userrole`"
            + "<set>"
            + "<if test=\"user.role != null\">role=#{user.role},</if>"
            + "<if test=\"user.qq != null\">qq=#{user.qq},</if>"
            + "<if test=\"user.legacyUname != null\">legacy_uname=#{user.legacyUname},</if>"
            + "<if test=\"user.currentUname != null\">current_uname=#{user.currentUname},</if>"
            + "<if test=\"user.banned != null\">is_banned=#{user.banned},</if>"
            + "<if test=\"user.repeatCount != null\">repeat_count=#{user.repeatCount},</if>"
            + "<if test=\"user.speakingCount != null\">speaking_count=#{user.speakingCount},</if>"
            + "<if test=\"user.mode != null\">mode=#{user.mode},</if>"
            + "<if test=\"user.mainRole != null\">main_role=#{user.mainRole},</if>"
            + "<if test=\"user.useEloBorder != null\">use_elo_border=#{user.useEloBorder},</if>"
            + "<if test=\"user.lastActiveDate != null\">last_active_date=#{user.lastActiveDate},</if>"
            + "</set>"
            + " where `user_id` = #{user.userId}" + "</script>")
    Integer updateUser(@Param("user") UserRoleEntity user);

    /**
     * Add user integer.
     *
     * @param user the user
     * @return the integer
     */
    @Insert("INSERT INTO `userrole` VALUES (null," +
            "#{user.userId}," +
            "#{user.role}," +
            "#{user.qq}," +
            "#{user.legacyUname}," +
            "#{user.currentUname}," +
            "#{user.banned}," +
            "#{user.repeatCount}," +
            "#{user.speakingCount}," +
            "#{user.mode}," +
            "#{user.mainRole}," +
            "#{user.useEloBorder}," +
            "#{user.lastActiveDate})")
    Integer addUser(@Param("user") UserRoleEntity user);


}
