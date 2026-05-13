package com.eauction.servlet;

import com.eauction.dao.ProductDAO;
import com.eauction.model.Product;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

/**
 * GET /search?q=keyword&category=Electronics&sort=endTime
 *
 * Forwards to jsp/buyer/search.jsp with attribute "results".
 * Accessible to any authenticated user.
 */
@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String keyword  = req.getParameter("q");
        String category = req.getParameter("category");
        String sort     = req.getParameter("sort");   // "endTime" | "price" | "name"

        // Sanitise
        if (keyword  != null) keyword  = keyword.trim();
        if (category != null) category = category.trim();
        if (sort     == null) sort     = "endTime";

        List<Product> results = productDAO.searchProducts(keyword, category, sort);

        req.setAttribute("results",         results);
        req.setAttribute("searchKeyword",   keyword);
        req.setAttribute("searchCategory",  category);
        req.setAttribute("searchSort",      sort);

        req.getRequestDispatcher("/jsp/buyer/search.jsp").forward(req, resp);
    }
}
