package org.live.live.controller;

import org.live.common.constants.SystemConfigConstants;
import org.live.common.response.ResponseModel;
import org.live.common.response.SimpleResponseModel;
import org.live.common.support.ServletContextHolder;
import org.live.live.entity.LiveCategory;
import org.live.live.service.LiveCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 *  直播房间分类的controller
 * Created by Mr.wang on 2017/3/29.
 */
@Controller
@RequestMapping("/live")
public class LiveCategoryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveCategoryController.class) ;

    @Resource
    private LiveCategoryService categoryService ;


    /**
     * 跳转到房间分类的页面
     * @return
     */
    @RequestMapping(value = "/category", method = RequestMethod.GET)
    public String toRoomCategory(HttpServletRequest request) {

        try {
            //List<LiveCategory> liveCategorys = categoryService.findAll();
            //request.setAttribute("liveCategorys", liveCategorys) ;
        } catch (Exception e) {
            LOGGER.error("查询直播房间分类时发生异常", e) ;
        }
        return "live/category" ;
    }

    @RequestMapping(value="/category/serialNo", method = RequestMethod.GET)
    @ResponseBody
    public ResponseModel<Object> findMaxSerialNo() {
        ResponseModel<Object>  model = new SimpleResponseModel<Object>() ;
        try {
            Integer serialNo = categoryService.findMaxSerialNo() ;
            if(serialNo == 0) serialNo = 1 ;
            model.setData(serialNo) ;
            model.success() ;
        } catch (Exception e) {
            LOGGER.error("查询直播分类的最大serialNo出现异常", e) ;
            model.error() ;
        }
        return model ;
    }

    /**
     *  添加直播分类
     * @param file
     * @param liveCategory
     * @param enabledNum 是否启用的标记
     * @return
     */
    @RequestMapping(value="/category",method = RequestMethod.POST)
    @ResponseBody
    public ResponseModel<Object> addLiveCategory(MultipartFile file, LiveCategory liveCategory, @RequestParam("enabled") Integer enabledNum) {

        ResponseModel<Object> model = new SimpleResponseModel<Object>() ;
        try {
            if(file == null) {
                model.setMessage("上传失败") ;
                model.error() ;
                return model ;
            }

            if(enabledNum == null || enabledNum == 0) liveCategory.setEnabled(false) ;    //启用状态
                String prefixUpload = ServletContextHolder.getAttribute(SystemConfigConstants.REAL_UPLOAD_FILE_DIR_KEY) ;
                LOGGER.debug("文件上传路径的前缀：---> "+ prefixUpload) ;





        } catch (Exception e) {

        }

        return model ;

    }




}