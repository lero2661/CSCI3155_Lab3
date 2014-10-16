object Lab3 extends jsy.util.JsyApplication {
  import jsy.lab3.ast._
  
  /*
   * CSCI 3155: Lab 3 
   * <Your Name>
   * 
   * Partner: <Your Partner's Name>
   * Collaborators: <Any Collaborators>
   */

  /*
   * Fill in the appropriate portions above by replacing things delimited
   * by '<'... '>'.
   * 
   * Replace 'YourIdentiKey' in the object name above with your IdentiKey.
   * 
   * Replace the 'throw new UnsupportedOperationException' expression with
   * your code in each function.
   * 
   * Do not make other modifications to this template, such as
   * - adding "extends App" or "extends Application" to your Lab object,
   * - adding a "main" method, and
   * - leaving any failing asserts.
   * 
   * Your lab will not be graded if it does not compile.
   * 
   * This template compiles without error. Before you submit comment out any
   * code that does not compile or causes a failing assert.  Simply put in a
   * 'throws new UnsupportedOperationException' as needed to get something
   * that compiles without error.
   */
  
  type Env = Map[String, Expr]
  val emp: Env = Map()
  def get(env: Env, x: String): Expr = env(x)
  def extend(env: Env, x: String, v: Expr): Env = {
    require(isValue(v))
    env + (x -> v)
  }
  
  def toNumber(v: Expr): Double = {
    require(isValue(v))
    (v: @unchecked) match {
      case N(n) => n
      case B(false) => 0
      case B(true) => 1
      case Undefined => Double.NaN
      case S(s) => try s.toDouble catch { case _: Throwable => Double.NaN }
      case Function(_, _, _) => Double.NaN
    }
  }
  
  def toBoolean(v: Expr): Boolean = {
    require(isValue(v))
    (v: @unchecked) match {
      case N(n) if (n compare 0.0) == 0 || (n compare -0.0) == 0 || n.isNaN => false
      case N(_) => true
      case B(b) => b
      case Undefined => false
      case S("") => false
      case S(_) => true
      case Function(_, _, _) => true
    }
  }
  
  def toStr(v: Expr): String = {
    require(isValue(v))
    (v: @unchecked) match {
      case N(n) => if (n.isWhole) "%.0f" format n else n.toString
      case B(b) => b.toString
      case Undefined => "undefined"
      case S(s) => s
      case Function(_, _, _) => "function"
    }
  }

  /*
   * Helper function that implements the semantics of inequality
   * operators Lt, Le, Gt, and Ge on values.
   */
  def inequalityVal(bop: Bop, v1: Expr, v2: Expr): Boolean = {
	require(isValue(v1))
	require(isValue(v2))
	require(bop == Lt || bop == Le || bop == Gt || bop == Ge)
    (v1, v2) match {
      case (S(s1), S(s2)) =>
        (bop: @unchecked) match {
          case Lt => s1 < s2
          case Le => s1 <= s2
          case Gt => s1 > s2
          case Ge => s1 >= s2
        }
      case _ =>
        val (n1, n2) = (toNumber(v1), toNumber(v2))
        (bop: @unchecked) match {
          case Lt => n1 < n2
          case Le => n1 <= n2
          case Gt => n1 > n2
          case Ge => n1 >= n2
        }
    }
  }


  /* Big-Step Interpreter with Dynamic Scoping */
  
  /*
   * This code is a reference implementation of JavaScripty without
   * strings and functions (i.e., Lab 2).  You are to welcome to
   * replace it with your code from Lab 2.
   */
  def eval(env: Env, e: Expr): Expr = {
    def eToN(e: Expr): Double = toNumber(eval(env, e))
    def eToB(e: Expr): Boolean = toBoolean(eval(env, e))
    def eToVal(e: Expr): Expr = eval(env, e)
    e match {
      /* Base Cases */
      case _ if isValue(e) => e
      case Var(x) => get(env, x)
      
      /* Inductive Cases */
      case Print(e1) => println(pretty(eval(env, e1))); Undefined
      
      case Unary(Neg, e1) => N(- eToN(e1))
      case Unary(Not, e1) => B(! eToB(e1))
      
      case Binary(Plus, e1, e2) => (eToVal(e1), eToVal(e2)) match {
        case (S(s1), v2) => S(s1 + toStr(v2))
        case (v1, S(s2)) => S(toStr(v1) + s2)
        case (v1, v2) => N(toNumber(v1) + toNumber(v2))
      }      
      case Binary(Minus, e1, e2) => N(eToN(e1) - eToN(e2))
      case Binary(Times, e1, e2) => N(eToN(e1) * eToN(e2))
      case Binary(Div, e1, e2) => N(eToN(e1) / eToN(e2))
      
      case Binary(Eq, e1, e2) => (eToVal(e1), eToVal(e2)) match {
        case (Function(_,_,_), _) => throw new DynamicTypeError(e)
        case (_, Function(_,_,_)) => throw new DynamicTypeError(e)
        case (S(s1), S(s2)) => B(s1 == s2)
        case (N(n1), N(n2)) => B(n1 == n2)
        case (B(b1), B(b2)) => B(b1 == b2)
        case (_, _) => B(false)
      }
      case Binary(Ne, e1, e2) => (eToVal(e1), eToVal(e2)) match {
        case (Function(_,_,_), _) => throw new DynamicTypeError(e)
        case (_, Function(_,_,_)) => throw new DynamicTypeError(e)
        case (S(s1), S(s2)) => B(s1 != s2) 
        case (N(n1), N(n2)) => B(n1 != n2)
        case (B(b1), B(b2)) => B(b1 != b2)
        case (_, _) => B(true)
      } 
      case Binary(bop @ (Lt|Le|Gt|Ge), e1, e2) => B(inequalityVal(bop, eToVal(e1), eToVal(e2)))
      
      case Binary(And, e1, e2) => 
        val v1 = eToVal(e1)
        if (toBoolean(v1)) eToVal(e2) else v1
      case Binary(Or, e1, e2) =>
        val v1 = eToVal(e1)
        if (toBoolean(v1)) v1 else eToVal(e2)
      
      case Binary(Seq, e1, e2) => eToVal(e1); eToVal(e2)
      
      case If(e1, e2, e3) => if (eToB(e1)) eToVal(e2) else eToVal(e3)
      
      case ConstDecl(x, e1, e2) => eval(extend(env, x, eToVal(e1)), e2)
      
      case Call(e1,e2) => (eval(env,e1),eval(env,e2)) match {
        case (Function(None,x,ebody),v2) => eval(extend(env,x,v2),ebody)
        case (v1 @ Function(Some(x1),x2,ebody),v2) => eval(extend(extend(env,x2,v2),x1,v1),ebody)
        case (_,_) => throw new DynamicTypeError(e)
      }
      case _ => throw new UnsupportedOperationException
    }
  }
    

  /* Small-Step Interpreter with Static Scoping */
  
  def substitute(e: Expr, v: Expr, x: String): Expr = {
    require(isValue(v))
    /* Simple helper that calls substitute on an expression
     * with the input value v and variable name x. */
    def subst(e: Expr): Expr = substitute(e, v, x)
    /* Body */
    e match {
      case N(_) | B(_) | Undefined | S(_) => e
      case Function(p, s, e1) => {
        if ((Some(x) == p) || (x ==s)) {
          return e;
        } else {
          return Function(p,s, subst(e1));
        }
      }
      case Binary(op, e1, e2) => op match {
        case (Eq | Ne) => e1 match {
          case Function(x,y,z) => throw new DynamicTypeError(e) 
          case _ => {
            return Binary(op, subst(e1), subst(e2));
          }
        }
        case _ => return Binary(op, subst(e1), subst(e2));
      }
      case Unary(op, e1) => return Unary(op, subst(e1))
      case Call(e1, e2) => return Call(subst(e1), subst(e2))
      case If(e1, e2, e3) => return If(subst(e1), subst(e2), subst(e3))
      case Print(e1) => Print(subst(e1))
      case Var(t) => if (t == x) v else e
      case ConstDecl(y, e1, e2) => ConstDecl(y, subst(e1), if (x == y) e2 else subst(e2))
      case _ => throw new UnsupportedOperationException
    }
  }
    
  def step(e: Expr): Expr = {
    e match {
      /* Base Cases: Do Rules */
      case Print(v1) if isValue(v1) => println(pretty(v1)); Undefined
      /* Inductive Cases: Search Rules */
      case Print(e1) => Print(step(e1))
      case Unary(op, e1) => op match {
        case Neg => {
          return N(-toNumber(e1));
        }
        case Not => {
          return B(!toBoolean(e1));
        }
      }
      case Binary(op, e1, e2) => op match {
        case Plus => (e1, e2) match {
          case (S(s1),e2) => return S(s1 + toStr(e2));
          case (e1, S(s2)) => return S(toStr(e1) + s2);
          case (_,_) => return N(toNumber(e1) + toNumber(e2));
        }
        case Minus => return N(toNumber(e1) - toNumber(e2));
        case Times => return N(toNumber(e1) * toNumber(e2));
        case Div => return N(toNumber(e1) / toNumber(e2));
        case Lt => (e1, e2) match {
          case (N(e1), N(e2)) => return B(e1 < e2);
          case (S(e1), S(e2)) => return B(e1 < e2);
          case (N(e1), S(e2)) => return B(toStr(N(e1)) < e2);
          case (S(e1), N(e2)) => return B(e1 < toStr(N(e2)));
          case (_,_) => throw new UnsupportedOperationException
        }
        case Le => (e1, e2) match {
          case (N(e1), N(e2)) => return B(e1 <= e2);
          case (S(e1), S(e2)) => return B(e1 <= e2);
          case (N(e1), S(e2)) => return B(toStr(N(e1)) <= e2);
          case (S(e1), N(e2)) => return B(e1 <= toStr(N(e2)));
          case (_,_) => throw new UnsupportedOperationException
        }
        case Gt => (e1, e2) match {
          case (N(e1), N(e2)) => return B(e1 > e2);
          case (S(e1), S(e2)) => return B(e1 > e2);
          case (N(e1), S(e2)) => return B(toStr(N(e1)) > e2);
          case (S(e1), N(e2)) => return B(e1 > toStr(N(e2)));
          case (_,_) => throw new UnsupportedOperationException
        }
        case Ge => (e1, e2) match {
          case (N(e1), N(e2)) => return B(e1 >= e2);
          case (S(e1), S(e2)) => return B(e1 >= e2);
          case (N(e1), S(e2)) => return B(toStr(N(e1)) >= e2);
          case (S(e1), N(e2)) => return B(e1 >= toStr(N(e2)));
          case (_,_) => throw new UnsupportedOperationException
        }
        case Eq => (e1, e2) match {
          case (Function(x, y, z), e2) => throw new DynamicTypeError(e)
          case (e1, Function(x, y, z)) => throw new DynamicTypeError(e)
          case (_,_) => return B(e1 == e2)
        }
        case Ne => (e1, e2) match {
          case (Function(x, y, z), e2) => throw new DynamicTypeError(e)
          case (e1, Function(x, y, z)) => throw new DynamicTypeError(e)
          case (_,_) => return B(e1 != e2)
        }
        case And => if (toBoolean(e1)) e2 else e1
        case Or => if (toBoolean(e1)) e1 else e2
        case Seq => e2
        case _ => throw new UnsupportedOperationException
      }
      case If(v1, e1, e2) => if (toBoolean(v1)) e1 else e2
      case Call(e1, e2) if ((isValue(e1) && isValue(e2))) => e1 match {
        case Function(Some(p), x, e3 ) => return substitute(substitute(e3, e1, p), e2, x);
        case Function(None, x, e3 ) => return substitute(e3, e2, x);
        case _ => throw new DynamicTypeError(e);
      }
      case Call(v1, e2) if (isValue(v1)) => v1 match {
        case Function(x, y, z) => Call(v1, step(e2));
        case _ => throw new DynamicTypeError(e);
      }
      case Call(e1, e2) => Call(step(e1), e2)
      /* Cases that should never match. Your cases above should ensure this. */
      case Var(_) => throw new AssertionError("Gremlins: internal error, not closed expression.")
      case N(_) | B(_) | Undefined | S(_) | Function(_, _, _) => throw new AssertionError("Gremlins: internal error, step should not be called on values.");
    }
  }
  

  /* External Interfaces */
  
  this.debug = true // comment this out or set to false if you don't want print debugging information
  
  // Interface to run your big-step interpreter starting from an empty environment and print out
  // the test input if debugging.
  def evaluate(e: Expr): Expr = {
    require(closed(e))
    if (debug) {
      println("------------------------------------------------------------")
      println("Evaluating with eval ...")
      println("\nExpression:\n  " + e)
    }
    val v = eval(emp, e)
    if (debug) {
      println("Value: " + v)
    }
    v
  } 
  
  // Convenience to pass in a jsy expression as a string.
  def evaluate(s: String): Expr = evaluate(jsy.lab3.Parser.parse(s))
   
  // Interface to run your small-step interpreter and print out the steps of evaluation if debugging. 
  def iterateStep(e: Expr): Expr = {
    require(closed(e))
    def loop(e: Expr, n: Int): Expr = {
      if (debug) { println("Step %s: %s".format(n, e)) }
      if (isValue(e)) e else loop(step(e), n + 1)
    }
    if (debug) {
      println("------------------------------------------------------------")
      println("Evaluating with step ...")
    }
    val v = loop(e, 0)
    if (debug) {
      println("Value: " + v)
    }
    v
  }

  // Convenience to pass in a jsy expression as a string.
  def iterateStep(s: String): Expr = iterateStep(jsy.lab3.Parser.parse(s))
  
  // Interface for main
  def processFile(file: java.io.File) {
    if (debug) {
      println("============================================================")
      println("File: " + file.getName)
      println("Parsing ...")
    }
    
    val expr =
      handle(None: Option[Expr]) {Some{
        jsy.lab3.Parser.parseFile(file)
      }} getOrElse {
        return
      }
    
    handle() {
      val v = evaluate(expr)
      println(pretty(v))
    }
    
    handle() {
      val v1 = iterateStep(expr)
      println(pretty(v1))
    }
  }
    
}
