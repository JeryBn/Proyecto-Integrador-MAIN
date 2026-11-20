import logging
from uuid import uuid4

logger = logging.getLogger(__name__)


class ApiRequestTrace:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        if not request.path.startswith('/api/'):
            return self.get_response(request)
        request_id = str(uuid4())
        response = self.get_response(request)
        response['X-Request-ID'] = request_id
        if response.status_code >= 500:
            logger.warning('API failure requestId=%s status=%s', request_id, response.status_code)
        return response
