package org.example.restfulbooker.support;

import org.example.restfulbooker.client.RestfulBookerClient;
import org.example.restfulbooker.reporting.HtmlReportExtension;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Common setup for API tests. Endpoint tests inherit the configured client instead
 * of creating duplicate request setup in every test class.
 */
@ExtendWith(HtmlReportExtension.class)
public abstract class BaseApiTest {

    protected final RestfulBookerClient api = new RestfulBookerClient();
}
