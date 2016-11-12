# telegram-bots

Welcome to telegram-bots!

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

## Contribution policy

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).
