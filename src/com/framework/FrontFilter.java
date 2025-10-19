package com.framework;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;


public class FrontFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String servletPath = request.getServletPath();
        ServletContext servletContext = request.getServletContext();

        // Vérifie si la ressource (fichier ou servlet) existe
        if (servletContext.getResource(servletPath) != null) {
            // Ressource trouvée → on continue la chaîne
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            // Ressource non trouvée → on affiche le chemin demandé
            response.setContentType("text/plain;charset=UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.println("Ressource introuvable : " + servletPath);
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Rien à initialiser ici
    }

    @Override
    public void destroy() {
        // Rien à nettoyer ici
    }
}
