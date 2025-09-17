# Documentation

## Commands

### `.execute`

Similar to the vanilla `/execute` command, but for Meteor Client.

**Subcommands**:
- `run <command>`: The command to execute, redirects to the Meteor Client dispatcher, so can be any Meteor Client comamnd.
- `if <script>`: Only proceeds with execution if the Starscript script is evaluated as `true`.
- `unless <script>`: Only proceeds with execution if the Starscript script is not evaluated as `true`.
- `delayed <delay> (ticks|millis|seconds)`: Suspends execution for the given delay. Subcommands following `delayed` will
also be evaluated after the delay. Using a delay with `millis` or `seconds` will cause the following subcommands and
command to be executed offthread, so take care to use only thread safe commands.
- `store (result|success) <variable>`: Stores either the `int` result or `boolean` success of the command specified in
the `run` subcommand to the Starscript variable specified in `variable`

### `.schedule`

`.schedule <delay> (ticks|millis|seconds) <command>`
See `.execute delayed` documentation for more information

### `.starscript`

- `.starscript eval <expression>` evaluates the expression and outputs the result
- `.starscript get <variable>` gets the given variable from Starscript globals and outputs the value

# License

Licensed All Rights Reserved.
