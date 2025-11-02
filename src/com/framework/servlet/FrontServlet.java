package com.framework.servlet;

import com.framework.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

public class FrontServlet extends HttpServlet {

    private Map<String, MethodRoute> routes;

    @Override
    public void init() throws ServletException {
        try {
            String packageControllers = "com.cousin.controller";
            ControllerScanner scanner = new ControllerScanner(packageControllers);
            scanner.afficherLesControllersEtRoutes();
            routes = scanner.getRoutes(); // On garde toutes les routes trouvées
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des contrôleurs", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        String path = request.getRequestURI().substring(request.getContextPath().length());
        out.println("<h1>URL demandée : " + path + "</h1>");

        try {
            MethodRoute methodRoute = routes.get(path);
            if (methodRoute != null) {
                Method method = methodRoute.getMethod();
                Object controller = methodRoute.getControllerInstance();
                Object result = method.invoke(controller);

                out.println("<h2>Résultat de la méthode :</h2>");
                out.println("<p>" + result + "</p>");
            } else {
                out.println("<h2>Aucune route trouvée pour cette URL</h2>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
}
