# Telegram Bot API for scala

This project aims to provide convenient way to write Telegram bots in scala, without strict dependency on concrete json or http library.

## Getting started

```
$ sbt
> codegen/run
> compile
```

## Developer notes
* Json4s doesn't play nice with type aliases. they have issue for that. Okay... 

## Todos
- [X] Extract return type for methods
- [X] InlineQueryResult/InputMessageContent are sealed traits and concrete case classes extends it
- [X] Type alias `type ChatId = Either[Long, String]`
- [X] Derive trait from OrType with more than 2 cases
- [X] InputFile write
- [X] Write requests and data structures to separate files, with possibility to specify package name.
- [X] Specify names for BotApiRequest { def requestName: String }
- [ ] Extract "must be" and "can be", and such
- [ ] Enum support
- [ ] Add proper logging
- [ ] Improve testing. It would be great, if we could write one set of tests, and parametrize them with circe/json4s/play object, that contains encode[T]
- [ ] Extract common function for scala meta, like package declaration, and such.
- [ ] Figure out, where all common code should be. Should we generate it every time?

### About testing.
For json modules we need to test that all requests(methods) are serialized properly among all json libraries we support.

Each of these test should consist of following steps:
1. Construct request case class
2. Make fake response json object(reference representation): `ApiResponse` with result in it. Keep data for future check
3. Pass request to method which accepts `BotApiRequest`(base type)
4. Serialize request to json
5. Compare serialized request with reference representation of request 
6. Compare deserialized fake response with data we made at step 2.

Thus, we need reference json representation of all requests(methods) and responses. We can try to generate it.
It would be great to test different data with property testing.

## Contribution policy

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
