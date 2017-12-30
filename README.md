# project2-server

CS496 두번째 프로젝트를 위한 Clojure 서버입니다.

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## API Specification

### 유저 생성하기

유저 id로는 email id를 사용??

### 연락처 정보

###### URI : /user/:user-id/contacts

/user/1231252435/contacts

response

```json
{
  "page": [
    
  ]
  "content": [
    {
      "name": "류석영 교수님",
      "phone": "010-8153-1244"
    }
  ]
}
```



GET - 유저 연락처 정보를 가져옵니다

검색 방법
q로 검색 옵션을 json encoding해서 보냅니다.

/user/:user-id/photos

GET

```json
{
  content: [
    {
      "id": "124gd234",
      "name": "dog-jpg",
      "thumbnail": "http://domain/thumb.jpg",
      "url": "http://s3.amazon.com/12513twd4.jpg"
    },
    {
      "id": "124gd234",
      "name": "dog-jpg",
      "thumbnail": "http://domain/thumb.jpg",
      "url": "http://s3.amazon.com/12513twd4.jpg"
    }
  ]
}
```



POST

/user/125312f/photos

이미지 파일 하나 보내기??


## License

Copyright © 2017 FIXME
