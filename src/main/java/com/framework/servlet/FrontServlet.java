package com.framework.servlet;

import com.framework.model.ModelView;
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
            // Lire depuis le web.xml si tu veux rendre ça configurable :
            String packageControllers = getServletConfig().getInitParameter("controller-package");
            if (packageControllers == null) {
                packageControllers = "com.cousin.controller"; 
            }

            ControllerScanner scanner = new ControllerScanner(packageControllers);
            scanner.afficherLesControllersEtRoutes();

            this.routes = scanner.getRoutes();

            ServletContext context = getServletContext();
            context.setAttribute("controllerPackage", packageControllers);
            context.setAttribute("routes", this.routes);

            System.out.println("✅ Package contrôleur et routes enregistrés dans le ServletContext !");
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du framework", e);
        }
    }


    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String path = request.getRequestURI().substring(request.getContextPath().length());

        try {
            MethodRoute methodRoute = this.routes.get(path);
            if (methodRoute != null) {
                Method method = methodRoute.getMethod();
                Object controller = methodRoute.getControllerInstance();
                Object result = method.invoke(controller);

                if (result instanceof String) {
                    out.println((String) result); 
                } else if (result instanceof ModelView) {
                    ModelView mv = (ModelView) result;
                    mv.getData().forEach(request::setAttribute);
                    RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
                    dispatcher.forward(request, response);
                }
            } else {
                out.println("<h2>Aucune route trouvée pour cette URL</h2>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
}
