# Documentation

## Commands

### `.execute`

Similar to the vanilla `/execute` command, but for Meteor Client.

**Terminal Operations**
- `run <command>`: The Meteor Client command to execute
- `send <command>`: The regular command to execute

  **Modifiers**
- `if <script>`: Only proceeds with execution if the Starscript script is evaluated as `true`.
- `unless <script>`: Only proceeds with execution if the Starscript script is not evaluated as `true`.
- `delayed (ticks|millis|seconds) <delay>`: Suspends execution for the given delay. Subcommands following `delayed` will
  also be evaluated after the delay. Using a delay with `millis` or `seconds` will cause the following subcommands and
  command to be executed offthread, so take care to use only thread safe commands.
- `repeat <times> instant`: Immediately forks command execution X times.
- `repeat <times> (ticks|millis|seconds) <delay>`: Do command execution X times with the given delay between executions.
  Using a delay with `millis` or `seconds` will cause the following subcommands and command to be executed offthread, so
  take care to use only thread safe commands. Use `delayed` to add an initial delay.
- `store (result|success) <variable>`: Stores either the `int` result or `boolean` success of the command specified in
  the `run` subcommand to the Starscript variable specified in `variable`

### `.schedule`

`.schedule (ticks|millis|seconds) <delay> <command>`
See `.execute delayed` documentation for more information

### `.starscript`

- `.starscript eval <expression>` evaluates the expression and outputs the result
- `.starscript get <variable>` gets the given variable from Starscript globals and outputs the value

## Composition

Executed commands have 2 return values that can be accessed after execution:
1. success, either `true` or `false` depending on whether the command executed successfully (failures cause a red chat
error)
2. result, the meaning of which is different for every command.

Both of these can be accessed through either `.execute store result` or `.execute store success` and stored in a
Starscript variable, either for use in an expression or to conditionally execute another command through `.execute if` &
`.execute unless`.

If a command execution is forked, for example as part of `.execute repeat instant`, the result will be the sum of all
forked results.
If a command execution is delayed, for example as part of `.execute delayed` and`.execute repeat (ticks|millis|seconds)`
, the result will
be `1` immediately, and then the expected value after the delay has passed.

### Results

Due to clientside limitations, the `send` terminal operation will always have a result of `1` on success.

- `.starscript`: If the output can be parsed as an integer, return that as a result. Otherwise, return `1`.

# License

Licensed All Rights Reserved.
