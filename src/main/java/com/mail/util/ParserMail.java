package com.mail.util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

/**
 * @ClassName : ParserMail
 * @Description : 获取邮件内容
 * @Author : yangyang
 * @Date : 2018年3月7日 上午9:59:35
 * @Version : V1.0
 */
public class ParserMail {
	public static void main(String[] args) throws Exception {
		String result = getMessage("userName","password","subject");
		System.out.println(result);
	}
	
	/**
	 * @Title : getMessage
	 * @Description : 查找邮件
	 * @Date : 2018年3月7日 上午11:49:08
	 * @param userName : 邮箱用户名
	 * @param password : 邮箱密码
	 * @param subject : 邮件主题
	 * @throws Exception
	 */
	private static String getMessage(String userName,String password,String subject) throws Exception {
		String host = "pop3.163.com";
		Properties props = new Properties();
		int flag = 0;
		int count = 1;
		String result = "";
		
		// 设置邮件接收协议为pop3
		props.setProperty("mail.store.protocol", "pop3");
		props.setProperty("mail.pop3.host", host);
		Session session = Session.getInstance(props);
		// 获取当前时间
		long currentTime = System.currentTimeMillis();
		// 当前时间-30分钟
		long startTime = currentTime - 30*60*1000;
		// 当前时间+30分钟
		long endTime = currentTime + 30*60*1000;
		// 轮循半个小时
		while (System.currentTimeMillis() <= endTime && flag == 0) {
			Store store = session.getStore("pop3");
			// 连接要获取数据的邮箱 主机+用户名+密码
			store.connect(host, userName, password);
			Folder folder = store.getFolder("inbox");
			// 设置邮件可读可写
			folder.open(Folder.READ_WRITE);
			Message[] messages = folder.getMessages();
			for (int i = 0; i < messages.length; i++) {
				// 解析邮件主题
				String messageSubject = messages[i].getSubject();
				// 解析发件时间
				Date sentDate = messages[i].getSentDate();
				// 主题与时间匹配						
				if (subject.equals(messageSubject) && sentDate.getTime() >= startTime) { 
					System.out.println("轮循"+count+"次,找到邮件!邮件内容如下:");
					result = getAllMultipart(messages[i]);
					flag = 1;
					break;
				}
			}
			if (flag == 0) {
				System.out.println("轮循"+count+"次,未找到邮件!");
				count ++;
				// 休眠一分钟
				Thread.sleep(1000*60);
			}
			folder.close(true);
			store.close();
		}
		
		return result;
	}

	/**
	 * 解析综合数据
	 * 
	 * @param part
	 * @throws Exception
	 */
	private static String getAllMultipart(Part part) throws Exception {
		String contentType = part.getContentType();
		int index = contentType.indexOf("name");
		boolean conName = false;
		String result = "";
		
		if (index != -1) {
			conName = true;
		}
		// 判断part类型
		if (part.isMimeType("text/plain") && !conName) {
			System.out.println((String) part.getContent());
			result = (String) part.getContent();
		} else if (part.isMimeType("text/html") && !conName) {
			System.out.println((String) part.getContent());
			result = (String) part.getContent();
		} else if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			int counts = multipart.getCount();
			for (int i = 0; i < counts; i++) {
				// 递归获取数据
				getAllMultipart(multipart.getBodyPart(i));
				// 附件可能是截图或上传的(图片或其他数据)
				if (multipart.getBodyPart(i).getDisposition() != null) {
					// 附件为截图
					if (multipart.getBodyPart(i).isMimeType("image/*")) {
						InputStream is = multipart.getBodyPart(i).getInputStream();
						String name = multipart.getBodyPart(i).getFileName();
						String fileName;
						// 截图图片
						if (name.startsWith("=?")) {
							fileName = name.substring(name.lastIndexOf(".") - 1, name.lastIndexOf("?="));
						} else {
							// 上传图片
							fileName = name;
						}
						// 附件默认保存D盘
						FileOutputStream fos = new FileOutputStream("D:\\" + fileName);
						int len = 0;
						byte[] bys = new byte[1024];
						while ((len = is.read(bys)) != -1) {
							fos.write(bys, 0, len);
						}
						fos.close();
					} else {
						// 其他附件
						InputStream is = multipart.getBodyPart(i).getInputStream();
						String name = multipart.getBodyPart(i).getFileName();
						FileOutputStream fos = new FileOutputStream("D:\\" + name);
						int len = 0;
						byte[] bys = new byte[1024];
						while ((len = is.read(bys)) != -1) {
							fos.write(bys, 0, len);
						}
						fos.close();
					}
				}
			}
		} else if (part.isMimeType("message/rfc822")) {
			getAllMultipart((Part) part.getContent());
		}
		
		return result;
	}

	/**
	 * 解析附件内容
	 * 
	 * @param part
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private static void getAttachmentMultipart(Part part) throws Exception {
		if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			int count = multipart.getCount();
			for (int i = 0; i < count; i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (bodyPart.getDisposition() != null) {
					InputStream is = bodyPart.getInputStream();
					FileOutputStream fos = new FileOutputStream("路径+文件名");
					int len = 0;
					byte[] bys = new byte[1024];
					while ((len = is.read(bys)) != -1) {
						fos.write(bys, 0, len);
					}
					fos.close();
				}
			}
		}

	}

	/**
	 * 解析图片内容
	 * 
	 * @param part
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private static void getPicMultipart(Part part) throws Exception {
		if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			int count = multipart.getCount();
			for (int i = 0; i < count; i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (bodyPart.isMimeType("image/*")) {
					InputStream is = bodyPart.getInputStream();
					FileOutputStream fos = new FileOutputStream("路径+文件名");
					int len = 0;
					byte[] bys = new byte[1024];
					while ((len = is.read(bys)) != -1) {
						fos.write(bys, 0, len);
					}
					fos.close();
				}
			}
		}
	}

	/**
	 * 解析文本内容
	 * 
	 * @param part
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private static void getTextMultipart(Part part) throws Exception {
		if (part.isMimeType("text/html")) {
			String content = (String) part.getContent();
			System.out.println(content);
		} else if (part.isMimeType("text/plain")) {
			String content = (String) part.getContent();
			System.out.println(content);
		}
	}
	// 如果只是纯文本文件情况
	// String content = (String) messages[i].getContent();
	// MIME中包含文本情况
	// getTextMultipart(messages[i]);
	// MIME中包含图片情况
	// getPicMultipart(messages[i]);
	// MIME中包含附件情况
	// getAttachmentMultipart(messages[i]);
	// 解析综合数据情况
	//getAllMultipart(messages[i]);
}
