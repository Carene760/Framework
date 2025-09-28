package com.framework.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class FrontServlet extends HttpServlet {
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        out.println("<h1>Informations sur la requête</h1>");
        out.println("<p>URI : " + request.getRequestURI() + "</p>");
        out.println("<p>URL : " + request.getRequestURL() + "</p>");
        out.println("<p>Contexte : " + request.getContextPath() + "</p>");
        out.println("<p>Servlet Path : " + request.getServletPath() + "</p>");
        out.println("<p>Query String : " + request.getQueryString() + "</p>");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        service(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        service(req, resp);
    }
}