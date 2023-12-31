// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gitiles;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

abstract class AbstractHttpFilter implements Filter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    doFilter((HttpServletRequest) req, (HttpServletResponse) res, chain);
  }

  @Override
  // Allow subclasses to throw ServletException.
  public void init(FilterConfig config) throws ServletException {
    // Default implementation does nothing.
  }

  @Override
  public void destroy() {
    // Default implementation does nothing.
  }

  /**
   * The FilterChain passed in to this method allows the Filter to pass on the request and response
   * to the next entity in the chain.
   *
   * @see #doFilter(ServletRequest, ServletResponse, FilterChain)
   */
  public abstract void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws IOException, ServletException;
}
