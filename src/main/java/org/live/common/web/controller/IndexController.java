package org.live.common.web.controller ;


import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.live.common.constants.Constants;
import org.live.common.constants.SystemConfigConstants;
import org.live.common.response.ResponseModel;
import org.live.common.response.SimpleResponseModel;
import org.live.common.support.ServletContextHolder;
import org.live.common.utils.HttpServletUtils;
import org.live.sys.entity.User;
import org.live.sys.service.MenuService;
import org.live.sys.service.UserService;
import org.live.sys.utils.MenuTreeUtils;
import org.live.sys.vo.SidebarNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

/**
 *  全局的url映射
 *
 *  @author Mr.wang
 */
@Controller
public class IndexController {

	private  static final Logger LOGGER = LoggerFactory.getLogger(IndexController.class) ;

	@Resource
	private UserService userService ;

	@Resource
	private MenuService menuService ;

	@Resource
	private ResourceLoader resourceLoader;

	/**
	 * 跳转到主界面
	 *
	 * @return
	 */
	@RequestMapping(value = {"/","/main"})
	public String main(HttpServletRequest request) {
		HttpSession session = request.getSession() ;
		User user = (User) session.getAttribute(Constants.CURRENT_LOGINUSER_KEY) ;
		User user1 = (User) SecurityUtils.getSubject().getSession().getAttribute(Constants.CURRENT_LOGINUSER_KEY);
		if(user != null) {
			List<SidebarNode> sidebarNodes = this.menuService.findSidebarNodesByUserId(user.getId()) ;
			String menuTreeMessage = MenuTreeUtils.appendSidebarBySidebarNodes(sidebarNodes);
			request.setAttribute("menuTreeMessage",menuTreeMessage) ;
			return "common/main" ;
		}
		return "common/main";   //重定向到登录页面
	}

	/**
	 *  跳转到登录页面
	 * @return
	 */
	@RequestMapping("/tologin")
	public String tologin() {
		return "redirect:/login" ;
	}

	/**
	 * 跳转到登录页面
	 * shiro的表单认证过滤器认证失败之后，跳转到这里，进行错误信息处理
	 * @return
	 */
	@RequestMapping(value={"/login","/index"})
	public String shiroFilterLogin(HttpServletRequest request, Model model) {
		//保存认证失败异常的全限命名
		final String shiroLoginFailure = (String) request.getAttribute("shiroLoginFailure") ;
		String errorMessage = null ;
		if(shiroLoginFailure != null) {
			if(StringUtils.equals(UnknownAccountException.class.getName(), shiroLoginFailure)) {	//用户不存在
				errorMessage = "用户名/密码错误" ;
			} else if(StringUtils.equals(IncorrectCredentialsException.class.getName(), shiroLoginFailure)) {	//密码错误
				errorMessage = "用户名/密码错误" ;
			} else if(StringUtils.equals(LockedAccountException.class.getName(), shiroLoginFailure)) {
				errorMessage = "账号被锁定" ;
			} else if(StringUtils.equals(AuthenticationException.class.getName(), shiroLoginFailure)) {
				errorMessage = "用户名/密码错误" ;
			}
			else {
				errorMessage = "认证用户错误" ;
			}
			model.addAttribute("errorMessage", errorMessage) ;
		}
		return "login" ;
	}

	/**
	 * 以ajax的方式登录
	 * @param request
	 * @return
	 */
	@RequestMapping("/ajaxlogin")
	@ResponseBody
	public ResponseModel<String> ajaxlogin(HttpServletRequest request, String username, String password) {
		ResponseModel<String> model = new SimpleResponseModel<String>() ;
		UsernamePasswordToken token =  new UsernamePasswordToken(StringUtils.trim(username),StringUtils.trim(password)) ;
		try {
			Subject subject = SecurityUtils.getSubject() ;
			subject.login(token) ;	//登录
			Session session = subject.getSession() ;
			User user = (User) session.getAttribute(Constants.CURRENT_LOGINUSER_KEY) ;
			if(user == null) {	//因为loginRealm有缓存，当同样的账号密码登录时，就不走loginRealm。所以session中就没有user
				user = this.userService.findByUsername(username).get(0) ;
				session.setAttribute(Constants.CURRENT_LOGINUSER_KEY, user) ;
				session.setAttribute(Constants.CURRENT_USERTYPE_KEY, user.getUserType()) ;
			}
			user.setLastLoginTime(new Date()) ;
			user.setLastLoginIp(HttpServletUtils.getIpAddr(request)) ;
			userService.save(user) ;
			model.success("登录成功") ;
		} catch(UnknownAccountException e) { //用户不存在
			LOGGER.debug(e.getMessage(),e);
			model.error("用户名/密码错误") ;
		} catch(IncorrectCredentialsException e) {	//密码错误
			LOGGER.debug(e.getMessage(),e);
			model.error("用户名/密码错误") ;
		} catch(ExcessiveAttemptsException e) {	//matcher记录信息
			model.error(e.getMessage()) ;
		} catch(LockedAccountException e) {
			LOGGER.debug(e.getMessage(),e) ;
			model.error("账号被锁定") ;
		} catch(AuthenticationException e) {
			LOGGER.debug(e.getMessage(),e);
			model.error("登录认证错误") ;
		} catch(Exception e) {
			LOGGER.error(e.getMessage(),e);
			model.error("服务器忙") ;
		}
		return model ;
	}

	/**
	 * 登出操作。
	 * @return
	 */
	@RequestMapping("/logout")
	public String shiroLogout() {
		SecurityUtils.getSubject().logout() ;	//登出
		return "redirect:/login" ;
	}

	/**
	 * 文件下载
	 * @return
	 */
	@RequestMapping(value="/upload/**")
	@ResponseBody
	public ResponseEntity<?> fileDownload(HttpServletRequest request, HttpServletResponse response) {
		try {

			//response.addHeader("Cache-Control","max-age=86400") ;	//缓存一天
			//文件下载的前缀
			String uploadPreifx = ServletContextHolder.getAttribute(SystemConfigConstants.DB_UPLOAD_FILE_PREFIX_KEY) ;
			//本地系统存储文件的前缀
			String realUploadFilePrefix = ServletContextHolder.getAttribute(SystemConfigConstants.REAL_UPLOAD_FILE_DIR_KEY) ;

			String path = request.getRequestURI() ;
			//去掉前缀upload
			path = path.replaceFirst(uploadPreifx, "") ;
			return ResponseEntity.ok(resourceLoader.getResource("file:"+ Paths.get(realUploadFilePrefix, path))) ;
		} catch (Exception e) {
			LOGGER.error("文件下载异常", e);
		}
		return null ;
	}



}
