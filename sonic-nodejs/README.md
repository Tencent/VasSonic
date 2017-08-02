### 1、Introduction

This is the server part of Sonic Project.

### 2、Step

1）Node Version > 7.0

2）install **sonic_differ** module


```Node.js
npm install sonic_differ --save
```

3）import **sonic_differ** module

```Node.js
const sonic_differ = require('sonic_differ');
```

4）Intercept and process data from server in Sonic mode.

i）First, create a Sonic cache struct like following code.

```Node.js
let sonic = {
    buffer: [],
    write: function (chunk, encoding) {
        let buffer = chunk;
        let ecode = encoding || 'utf8';
        if (!Buffer.isBuffer(chunk)) {
            buffer = new Buffer(chunk, ecode);
        }
        sonic.buffer.push(buffer);
    }
};
```

ii）Second, Intercept the data from server and use `sonic_differ` module to process

```Node.js
response.on('data', (chunk, encoding) => {
    sonic.write(chunk, encoding)
});
response.on('end', () => {
    let result = differ(ctx, Buffer.concat(sonic.buffer));
    sonic.buffer = [];
    if (result.cache) {
        //304 Not Modified, return nothing.
        return ''
    } else {
        //other Sonic status.
        return result.data
    }
});
```