package com.hotent.platform.controller.system;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import com.hotent.core.annotion.Action;
import com.hotent.core.util.ContextUtil;
import com.hotent.core.web.ResultMessage;
import com.hotent.core.web.controller.BaseController;
import com.hotent.core.web.query.QueryFilter;
import com.hotent.core.web.util.RequestUtil;
import com.hotent.platform.auth.ISysUser;
import com.hotent.platform.model.system.MessageRead;
import com.hotent.platform.model.system.MessageReply;
import com.hotent.platform.model.system.MessageSend;
import com.hotent.platform.service.system.MessageReadService;
import com.hotent.platform.service.system.MessageReplyService;
import com.hotent.platform.service.system.MessageSendService;

/**
 * 对象功能:发送消息 控制器类
 */
@Controller
@RequestMapping("/platform/system/messageSend/")
public class MessageSendController extends BaseController
{
	@Resource
	private MessageSendService sendService;
	@Resource
	private MessageReadService readService;
	@Resource
	Properties configproperties;
	@Resource
	private MessageReplyService replyService;
	
	/**
	 * 发送和接收列表框架
	 * @param request
	 * @param response
	 * @param page
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("form")
	@Action(description="发送和接收列表框架")
	public ModelAndView form(HttpServletRequest request,HttpServletResponse response) throws Exception
	{	
		ISysUser curUser = ContextUtil.getCurrentUser();
		Long userId=curUser.getUserId();
		ModelAndView mv=this.getAutoView().addObject("userId",userId);		
		return mv;
	}
	
	/**
	 * 取得发送消息分页列表
	 * @param request
	 * @param response
	 * @param page
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("list")
	@Action(description="查看发送消息分页列表")
	public ModelAndView list(HttpServletRequest request,HttpServletResponse response) throws Exception
	{	
		Long userId=ContextUtil.getCurrentUserId();
		Date now =new Date();
		Long longTime =now.getTime();
		String spanTime=configproperties.getProperty("send.timeout");
		QueryFilter queryFilter=new QueryFilter(request,"messageSendItem");
		queryFilter.addFilter("userId", userId);
		List<MessageSend> list=sendService.getAll(queryFilter);
		ModelAndView mv=this.getAutoView()
				.addObject("messageSendList",list)
				.addObject("longTime", longTime) 
				.addObject("spanTime", spanTime);
		
		return mv;
	}
	
	/**
	 * 删除发送消息
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("del")
	@Action(description="删除发送消息")
	public void del(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		String preUrl= RequestUtil.getPrePage(request);
		ResultMessage message=null;
		try{
			Long[] lAryId =RequestUtil.getLongAryByStr(request, "id");
			sendService.delByIds(lAryId);
			message=new ResultMessage(ResultMessage.Success, "删除发送消息成功!");
		}
		catch(Exception ex){
			message=new ResultMessage(ResultMessage.Fail, "删除失败:" + ex.getMessage());
		}
		addMessage(message, request);
		response.sendRedirect(preUrl);
	}

	@RequestMapping("edit")
	@Action(description="编辑发送消息")
	public ModelAndView edit(HttpServletRequest request) throws Exception
	{
		Long id=RequestUtil.getLong(request,"id");
		ISysUser curUser = ContextUtil.getCurrentUser();
		Long userId=curUser.getUserId();
		String returnUrl=RequestUtil.getPrePage(request);
		MessageSend messageSend=null;
		if(id!=0){
			 messageSend= sendService.getById(id);
		}else{
			messageSend=new MessageSend();
		}
		return getAutoView().addObject("messageSend",messageSend)
				.addObject("userId",userId)
				.addObject("returnUrl", returnUrl);
	}

	/**
	 * 取得发送消息明细
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("get")
	@Action(description="查看发送消息明细")
	public ModelAndView get(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long id=RequestUtil.getLong(request,"id");
		String flag=RequestUtil.getString(request,"flag");
		ISysUser curUser = ContextUtil.getCurrentUser();
		Long userId=curUser.getUserId();
		MessageSend messageSend = sendService.getById(id);	
		readService.addMessageRead(messageSend.getId(), curUser);

		return getAutoView()
				.addObject("messageSend", messageSend)
				.addObject("flag", flag)
				.addObject("userId",userId);
	}
	
	@RequestMapping("readMsgDialog")
	@Action(description="查看未读信息")
	public ModelAndView readMsgDialog(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long id=RequestUtil.getLong(request,"id");
		ISysUser sysUser = ContextUtil.getCurrentUser();
		int size = 0;
		MessageSend messageSend = null;
		if(id!=0){
			messageSend = sendService.getById(id);
		}else{
			List<MessageSend> list=sendService.getNotReadMsg(sysUser.getUserId());
			messageSend = list.get(0);
		}
		
		readService.addMessageRead(messageSend.getId(), sysUser);
		
		// 生成msgReply对象，方便提交
		MessageReply msgReply = new MessageReply();
		msgReply.setMessageId(messageSend.getId());
		msgReply.setIsPrivate(new Short("1"));
		
		return getAutoView().addObject("messageSend", messageSend).addObject("flag", size>1?true:false)
				.addObject("msgReply", msgReply);
	}
	
	@RequestMapping("notReadMsg")
	@Action(description="未读信息条数")
	public void notReadMsg(HttpServletResponse response) throws IOException{

		List<MessageSend> list=sendService.getNotReadMsg(ContextUtil.getCurrentUserId());
		
		response.getWriter().print(list.size());
		
	}
	
	@RequestMapping("readDetail")
	@Action(description="查看已读明细")
	public ModelAndView readDetail(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long id=RequestUtil.getLong(request,"id");
		int conReply = RequestUtil.getInt(request, "canReply");
		// 获得已读此消息的人员
		List<MessageRead> readlist=readService.getReadByMsgId(id);	
		return getAutoView().addObject("readlist", readlist).addObject("canReply", conReply);
	}
	
	@RequestMapping("replyDetail")
	@Action(description="查看回复明细")
	public ModelAndView replyDetail(HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		long id=RequestUtil.getLong(request,"id");

		// 获得已回复此消息的人员
		List<MessageReply> replylist=replyService.getReplyByMsgId(id);
		return getAutoView().addObject("replylist", replylist);
	}
	
}
