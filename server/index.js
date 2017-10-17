/**
 * 전투체육 매칭 Backend
 * 
 * 이 App에서는 매칭 데이터를 받아 처리한 뒤,
 * Client에서 원하는 때 받아 쓸 수 있도록 처리해두고
 * 필요할 때 request하여 받아쓸 수 있게 합니다.
 * 
 * @version 1.0.0
 * @description 전투체육 매칭 Application Backend
 * @author 김범수, 안정인, 임대인
 */

var http = require('http'),
    express = require('express'),
    cookieParser = require('cookie-parser'),
    session = require('express-session'),
    expressErrorHandler = require('express-error-handler'),
    bodyParser = require('body-parser'),
    static = require('serve-static'),
    path = require('path'),
    DatabaseManager = require('./lib/DatabaseManager'),
    UserManager = require('./lib/UserManager');

// express 이용 HTTP 서버 설정
var app = express();
app.set('port', process.env.PORT || 14403);
app.set('mongoose-reconnect-max', 5);

// express Router 이용 Request routing
var router = express.Router();

// 라우터 설정
// 사용자 추가 (회원가입)
router.route('/process/registerUser').post(function (req, res) {
    var userInfo = {
        id: req.body.id,
        password: req.body.password,
        name: req.body.name,
        rank: req.body.rank,
        unit: req.body.unit,
        gender: req.body.gender,
        favoriteEvent: req.body.favoriteEvent,
        description: req.body.description
    };

    // 정보 중 하나라도 빠졌을 시 오류
    for (var key in userInfo)
        if (!userInfo[key]) {
            res.json({
                result: false,
                reason: 'MissingValuesException'
            });
            res.end();
            return;
        }

    // 가져온 정보를 MongoOSE 이용하여 DB에 저장
    DatabaseManager.createUser(userInfo, function (err) {
        if (err) {
            if (err.code == 11000) {
                console.log('[오류] 이미 존재하는 사용자에 대한 회원가입');
                res.json({
                    result: false,
                    reason: 'AlreadyExistingException'
                });
                res.end();
                return;
            } else {
                console.log('에러 발생!');
                console.dir(err);
                console.log('에러 출력 완료');
                throw err;
            }
        } else {
            console.log('[정보] 회원가입 완료: ID [%s]', userInfo.id);
            res.json({
                result: true,
                id: userInfo.id,
                name: userInfo.name
            });
            res.end();
            return;
        }
    });
});

router.route('/process/checkLoggedIn').get(function(req, res) {

    if(req.session.userInfo)
        res.json({
            logged_as: req.session.userInfo.id
        });
    else
        res.json({
            logged_as: false
        });

    res.end();
});

router.route('/process/loginUser').post(function (req, res) {
    var userInfo = {
        id: req.body.id,
        password: req.body.password
    };
    DatabaseManager.Model.user.authenticate(userInfo, function (result) {
        if(result.result) {
            console.log('[정보] 로그인 완료. 세션에 추가 중');
            req.session.userInfo = {
                id: userInfo.id
            };
        } else {
            console.log('[오류] 로그인 불가. 사유: %s', result.reason);
        }
        res.json(result);
        res.end();
    });
});

router.route('/process/logoutUser').get(function (req, res) {
    req.session.destroy(function(err) {
        if(err) throw err;
        res.json({
            result: true
        });
        res.end();

        console.log('[정보] 로그아웃 및 세션 초기화 완료');
    });
});

router.route('/process/getMatchList').get(function (req, res) {
    // 현재 진행중인 Match 목록
    res.json({ result: 'Router Works (getMatchList)' });
    res.end();
});

router.route('/process/requestMatch').post(function (req, res) {
    var matchInfo = {
        id: req.session.userInfo? req.session.userInfo.id : req.body.id,
        activityType: req.body.activityType,
        maxUsers: req.body.maxUsers,
        matchId: req.body.matchId || null
    };

    DatabaseManager.Model.matching.findMatch(matchInfo, function(err, result) {
        if(err) throw err;
        
        res.json(result);
        res.end();
    });
});

router.route('/process/heartbeat').get(function (req, res) {
    // Heartbeat
    res.json({ result: 'result' });
    res.end();
});

router.route('/process/checkExistingUser').post(function (req, res) {
    // 기존 회원 ID를 확인한다.
    var userInfo = {
        id: req.body.id
    };

    DatabaseManager.Model.user.findId(userInfo, function(result) {
        res.json(result);
        res.end();
    });
});

// Express에 각 미들웨어 적용 및 서버 시작
app.use(cookieParser());
app.use(session({
    secret: 'F$GKeE%tJaf($&#(SfGISf*%#n#@!zSWh9',
    resave: true,
    saveUninitialized: true
}));

app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.use(static(path.join(__dirname, 'public')));

app.use('/', router);

app.use(expressErrorHandler.httpError(404));
app.use(expressErrorHandler({
    static: {
        '404': './include/404.html'
    }
}));

// HTTP 서버 구동
http.createServer(app).listen(app.get('port'), function () {
    DatabaseManager.connectDB();
    console.log('[정보] 서버 시작됨. %d에서 listen 중', app.get('port'));
});