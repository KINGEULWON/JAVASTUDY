package board.action;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import board.model.BoardDao;
import board.model.BoardDto;


@WebServlet(
	urlPatterns="/upload", 
	initParams= {
		@WebInitParam(
			name="uploadPath", 
			value="C:\\uploadtest\\upload\\"
		)
	}
)
@MultipartConfig(maxFileSize = -1, maxRequestSize = -1, fileSizeThreshold = 1024)
public class UploadServlet extends HttpServlet {

	private static String uploadPath = ""; // 업로드 경로
	private File uploadDir = null;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		uploadPath = config.getInitParameter("uploadPath");
		System.out.println(uploadPath);
		uploadDir = new File(uploadPath);
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String[] fileList = uploadDir.list();
		for(int i = 0; i < fileList.length; i++) {
			System.out.println(fileList[i]);
		}
		req.setAttribute("fileList", fileList);
		req.getRequestDispatcher("/WEB-INF/board/writeForm.jsp").forward(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String contentType = request.getContentType();
		if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
			printPartInfo(request);
		} else {
			request.setAttribute("result", -1);
		}
		request.getRequestDispatcher("/board/writePro.jsp").forward(request, response);
	}

	private String printPartInfo(HttpServletRequest request) throws ServletException, IOException {
		Collection<Part> parts = null;
		List<BoardDto> uploadFileList = new ArrayList<>();
		BoardDto article = new BoardDto();	//데이터를 처리할 빈
		try {
			parts = request.getParts();
			for (Part part : parts) {
				File uploadDir = null;
				String fileName = null;
				long size = 0;
				String contentType = null;
				String value = null;
				
				contentType = part.getContentType();
				
				if (contentType == null) {
					value = readParameterValue(part);
					part.delete();
				} else if (contentType.startsWith("application/") || contentType.startsWith("image/") || contentType.startsWith("text/")) {
					fileName = getFileName(part);
					System.out.println("filename = " + fileName);
					size = part.getSize();
					System.out.println("size = " + size);
					
					article.setFilename(fileName);
					article.setFileSize(size);
					article.setContentType(contentType);

					if (size > 0) {
						part.write(uploadPath + fileName);
						part.delete();
					}
					uploadFileList.add(article);
				}
				System.out.println("part.name = " + part.getName());
				System.out.println("value = " + value);
				System.out.println("part.getHeader = " + part.getHeader("Content-Disposition"));
				System.out.println("contentType = " + contentType);
				System.out.println("filename = " + fileName);
				System.out.println("size = " + size);
				System.out.println("uploadPath:" + uploadDir + fileName);				
				System.out.println();
			}
			request.setCharacterEncoding("UTF-8");
			article.setNum(Integer.parseInt(request.getParameter("num")));
			article.setUploader(request.getParameter("uploader"));
			article.setEmail(request.getParameter("email"));
			article.setSubject(request.getParameter("subject"));
			article.setPass(request.getParameter("pass"));
			article.setRegdate(new Timestamp(System.currentTimeMillis()));
			article.setRef(Integer.parseInt(request.getParameter("ref")));
			article.setStep(Integer.parseInt(request.getParameter("step")));
			article.setDepth(Integer.parseInt(request.getParameter("depth")));
			article.setContent(request.getParameter("content"));
			article.setIp(request.getRemoteAddr());
			request.setAttribute("uploadFileList", uploadFileList);
			BoardDao dbPro = BoardDao.getInstance(); //DB 연결
			dbPro.insertArticle(article);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return "/board/writePro.jsp";
	}

	private String getFileName(Part part) throws UnsupportedEncodingException {
		for (String cd : part.getHeader("Content-Disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				String tmp = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
				tmp = tmp.substring(tmp.indexOf(":") + 1);
				return tmp;
			}
		}
		return null;
	}

	private String readParameterValue(Part part) throws IOException {
		InputStreamReader isr = new InputStreamReader(part.getInputStream(), "utf-8");
		char[] data = new char[512];
		int len = -1;
		StringBuilder builder = new StringBuilder();
		while ((len = isr.read(data)) != -1) {
			builder.append(data, 0, len);
		}
		return builder.toString();
	}
	
}
