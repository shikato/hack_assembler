package org.shikato.hack

import java.io.File 
import java.io.FileWriter
import java.io.PrintWriter 
import java.io.BufferedWriter

// TODO: HackAssemblerException 

fun main(args: Array<String>) { 

  if (args.size != 1) { 
      throw IllegalArgumentException("unexpected arguments.")
  }

  // todo try/catch
  val target = File(args[0])
  val lines = target.readLines().filter(String::isNotBlank).toList() 

  val parser: Parser = Parser(lines)
  val code: Code = Code() 
  val symbolTable: SymbolTable = SymbolTable()

  var currentRomAddress: Int = 0; 
  var currentRamAddress: Int = 16; 

  while (parser.hasMoreCommands()) {
    parser.advance()
    when (parser.commandType()) {
      CommandType.L_COMMAND -> { 
        symbolTable.addEntry(parser.symbol(), currentRomAddress);
      } 
      else -> {
        currentRomAddress++
      } 
    }
  } 

  parser.reset()

  val outFile = FileWriter("${target.nameWithoutExtension}.hack")
  val printWriter = PrintWriter(BufferedWriter(outFile)) 
  var commands: String = "" 
  while (parser.hasMoreCommands()) {
    parser.advance() 
    var command: String = ""
    when (parser.commandType()) {
      CommandType.A_COMMAND -> { 
        val symbol = parser.symbol()
        if (symbolTable.contains(symbol)) { 
          command = createABinary(Integer.toString(symbolTable.getAddress(symbol)))
        } else if (Character.isDigit(symbol[0])) { 
          command = createABinary(symbol)
        } else {
          symbolTable.addEntry(symbol, currentRamAddress);
          currentRamAddress++ 
          command = createABinary(Integer.toString(symbolTable.getAddress(symbol)))
        } 
      } 
      CommandType.C_COMMAND -> {
        command = "111" +
          code.comp(parser.comp()) +
          code.dest(parser.dest()) +
          code.jump(parser.jump()) +
          "\n" 
      } 
    } 
    commands += command
    printWriter.print(command)
  }

//  println(commands)
  printWriter.close()
}

fun createABinary(address: String): String { 
  return String.format("%16s", 
      Integer.toBinaryString(Integer.parseInt(address))).replace(" ", "0") +
    "\n"
}

enum class CommandType {
  A_COMMAND, C_COMMAND, L_COMMAND
}

class Parser(lines: List<String>) {
  val lines: List<String>
  var currentLine: String = ""
  var currentIndex: Int = 0
//  var currentComp: String = "" 
//  var currentDest: String = ""
//  var currentJump: String = ""
//  var currentSymbol: String = ""

  init {
    this.lines = lines
  } 

  fun hasMoreCommands(): Boolean {
    return if (this.currentIndex < this.lines.size) true else false
  }
 
  fun advance() { 
    var tmp: String
    do {
      tmp = lines.get(currentIndex)
      tmp = removeComment(tmp)
      tmp = removeSpace(tmp) 
      this.currentIndex++
    } while (tmp == "")

    this.currentLine = tmp
  }

  fun commandType(): CommandType { 
    this.currentLine.forEach { 
      return if (it == '@') { 
        CommandType.A_COMMAND
      } else if (it == '(') { 
        CommandType.L_COMMAND
      } else {
        CommandType.C_COMMAND
      }
    } 
    // todo: Excetion
    return CommandType.C_COMMAND
  }

  fun symbol(): String { 
    var splitRes: List<String> = this.currentLine.split("@") 
    return if (splitRes.size == 2) {
      splitRes[1]
    } else {
      this.currentLine.substring(1, this.currentLine.length - 1)
    } 
  } 

  fun dest(): String { 
    val splitRes: List<String> = this.currentLine.split("=")
    return if (splitRes.size == 2) {
      splitRes[0]
    } else {
      ""
    }
  }

  fun comp(): String { 
    var splitRes: List<String> = this.currentLine.split("=")
    return if (splitRes.size == 2) {
      splitRes[1]
    } else { 
      splitRes = this.currentLine.split(";")
      splitRes[0] 
    } 
  }

  fun jump(): String { 
    val splitRes: List<String> = this.currentLine.split(";")
    return if (splitRes.size == 2) {
      splitRes[1]
    } else {
      ""
    }
  }

  fun reset() { 
    this.currentLine = ""
    this.currentIndex = 0
  }

  private fun removeComment(target: String): String {
    return target.replace("//.*".toRegex(), "") 
  }

  private fun removeSpace(target: String): String {
    return target.replace("\\s".toRegex(), "") 
  } 
}

class Code { 
  val comp = mapOf(
      "0" to "0101010", 
      "1" to "0111111",
      "-1" to "0111010",
      "D" to "0001100", 
      "A" to "0110000",
      "!D" to "0001101",
      "!A" to "0110001",
      "-D" to "0001111", 
      "-A" to "0110011",
      "D+1" to "0011111",
      "A+1" to "0110111",
      "D-1" to "0001110", 
      "A-1" to "0110010",
      "D+A" to "0000010",
      "D-A" to "0010011",
      "A-D" to "0000111", 
      "D&A" to "0000000",
      "D|A" to "0010101",
      // a=1
      "M" to "1110000",
      "!M" to "1110001",
      "-M" to "1110011",
      "M+1" to "1110111",
      "M-1" to "1110010",
      "D+M" to "1000010",
      "D-M" to "1010011",
      "M-D" to "1000111", 
      "D&M" to "1000000",
      "D|M" to "1010101")

  val dest = mapOf(
      "" to "000",
      "M" to "001",
      "D" to "010",
      "MD" to "011",
      "A" to "100",
      "AM" to "101",
      "AD" to "110",
      "JMP" to "111")

  val jump = mapOf(
      "" to "000",
      "JGT" to "001",
      "JEQ" to "010",
      "JGE" to "011",
      "JLT" to "100",
      "JNE" to "101",
      "JLE" to "110",
      "JMP" to "111")

  fun dest(mnemonic: String): String {
    return if (dest.containsKey(mnemonic)) {
      dest[mnemonic]!!
    } else {
      throw IllegalArgumentException("non exsting mnemonic.")
    } 
  }

  fun comp(mnemonic: String): String { 
    return if (comp.containsKey(mnemonic)) {
      comp[mnemonic]!!
    } else {
      throw IllegalArgumentException("non exsting mnemonic.")
    }
  }

  fun jump(mnemonic: String): String { 
    return if (jump.containsKey(mnemonic)) {
      jump[mnemonic]!!
    } else {
      throw IllegalArgumentException("non exsting mnemonic.")
    }
  }
}

class SymbolTable { 

  val symbolEntities = mutableMapOf<String, Int>()

  init {
    this.symbolEntities["SP"] = 0
    this.symbolEntities["LCL"] = 1
    this.symbolEntities["ARG"] = 2
    this.symbolEntities["THIS"] = 3
    this.symbolEntities["THAT"] = 4
    this.symbolEntities["SCREEN"] = 16384
    this.symbolEntities["KBD"] = 24576
    this.symbolEntities["R0"] = 0
    this.symbolEntities["R1"] = 1
    this.symbolEntities["R2"] = 2
    this.symbolEntities["R3"] = 3
    this.symbolEntities["R4"] = 4
    this.symbolEntities["R5"] = 5
    this.symbolEntities["R6"] = 6
    this.symbolEntities["R7"] = 7
    this.symbolEntities["R8"] = 8
    this.symbolEntities["R9"] = 9
    this.symbolEntities["R10"] = 10
    this.symbolEntities["R11"] = 11
    this.symbolEntities["R12"] = 12
    this.symbolEntities["R13"] = 13
    this.symbolEntities["R14"] = 14
    this.symbolEntities["R15"] = 15
  }

  fun addEntry(symbol: String, address: Int) {
    this.symbolEntities[symbol] = address
  }

  fun contains(symbol: String): Boolean {
    return symbolEntities.containsKey(symbol)
  } 

  fun getAddress(symbol: String): Int {
    return if (symbolEntities.containsKey(symbol)) {
      symbolEntities[symbol]!!
    } else {
      throw IllegalArgumentException("non exsting symbol.")
    } 
  }
} 
