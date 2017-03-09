var port = process.env.PORT || 8000,
    Http = require('http'),
    Url = require('url'),
    datastore,
    handlers = {
      '/': function(req, res) {
        var response;
        switch(req.method) {
          case 'GET':
            return res.end(ok({ datastore: datastore }));
          case 'POST':
            return readBody(req)
              .then(JSON.parse)
              .then(function(message) {
                datastore.webapp_originating.waiting.push(message);
                res.end(ok({ added_message: message }));
              });
          case 'DELETE':
            resetDatastore();
            return res.end(ok());
          default: throw new Error('Unhandled method.');
        }
      },
      '/error': function(req, res) {
        return readBody(req)
          .then(JSON.parse)
          .then(function(requestedError) {
            datastore.errors.push(requestedError);
            res.end(ok({ added_error: requestedError }));
          });
      },
      '/auth': function(req, res) {
        switch(req.method) {
          case 'DELETE':
            delete datastore.auth;
            return res.end(ok());
          case 'POST':
            return readBody(req)
              .then(JSON.parse)
              .then(function(json) {
                datastore.auth = json;
                res.end(ok());
              });
          default: throw new Error('Unhandled method.');
        }
      },
      '/app': function(req, res) {
        if(!handleAuth(req, res, datastore.auth)) return;

        var storedReq;

        if(req.method === 'GET' || req.method === 'POST') {
          datastore.requests.unshift(storedReq = {
            useragent: req.headers['user-agent'],
            method: req.method,
            time: new Date().toString(),
          });
        }

        switch(req.method) {
          case 'GET':
            return res.end(ok({ 'medic-gateway': true }));
          case 'POST':
            // enforce expected headers
            assertHeader(req, 'Accept', 'application/json');
            assertHeader(req, 'Accept-Charset', 'utf-8');
            assertHeader(req, 'Accept-Encoding', 'gzip');
            assertHeader(req, 'Cache-Control', 'no-cache');
            assertHeader(req, 'Content-Type', 'application/json');

            if(datastore.errors.length) {
              throw new Error(datastore.errors.shift());
            }

            return readBody(req)
              .then(JSON.parse)
              .then(function(json) {
                storedReq.postBody = json;

                push(datastore.webapp_terminating, json.messages);
                push(datastore.status_updates, json.updates);

                res.end(JSON.stringify({
                  messages: datastore.webapp_originating.waiting,
                }));
                push(datastore.webapp_originating.passed_to_gateway,
                    datastore.webapp_originating.waiting);
                datastore.webapp_originating.waiting.length = 0;
              });
          default: throw new Error('Unhandled method.');
        }
      },
    };

function handleAuth(req, res, options) {
  var error, header;

  if(!options) return true;

  try {
    header = req.headers.authorization;
    header = header.split(' ').pop();
    header = new Buffer(header, 'base64').toString().split(':');

    if(header[0] === options.username &&
        header[1] === options.password) {
      return true;
    }
    error = 'username or password did not match';
  } catch(e) {
    error = e;
  }
  console.log('    Auth failed:', error);
  console.log('        headers:', req.headers);
  res.writeHead(401);
  res.end(JSON.stringify({ err:'Unauthorized' }, null, 2));
  return false;
}

function resetDatastore() {
  datastore = {
    requests: [],
    webapp_terminating: [],
    webapp_originating: {
      waiting: [],
      passed_to_gateway: [],
    },
    status_updates: [],
    errors: [],
  };
}

function ok(r) {
  if(!r) r = {};
  r.ok = true;
  return JSON.stringify(r, null, 2);
}

function readBody(req) {
  var body = '';
  return new Promise(function(resolve, reject) {
    req.on('data', function(data) {
      body += data.toString();
    });
    req.on('end', function() {
      resolve(body);
    });
    req.on('error', reject);
  });
}

function push(arr, vals) {
  arr.push.apply(arr, vals);
}

function assertHeader(req, key, expected) {
  var actual = req.headers[key.toLowerCase()];
  if(actual !== expected)
    throw new Error(
        'Bad value for header "' + key + '": ' +
        'expected "' + expected + '", ' +
        'but got "' + actual + '"');
}


resetDatastore();

Http.createServer(function(req, res) {
  var url = Url.parse(req.url),
      handler = handlers[url.pathname],
      requestBody = req.read();

  console.log(new Date(), req.method, req.url);

  function error(message) {
    var i, body = {
      err: message,
      method: req.method,
      url: url,
    };
    if(arguments.length > 1) {
      body.extras = Array.prototype.slice.call(arguments, 1);
    }
    res.writeHead(500);
    res.end(JSON.stringify(body, null, 2));
    console.log('    ERROR: ' + message);
  }

  if(!handler) {
    return error('Path not found for URL: ');
  }

  Promise.resolve()
    .then(function() {
      return handler(req, res);
    })
    .catch(function(e) {
      error(e.message);
    });
}).listen(port);

console.log('Listening on port ' + port + '...');
