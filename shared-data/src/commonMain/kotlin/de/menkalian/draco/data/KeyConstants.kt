@file:Suppress("FunctionName", "PropertyName", "PrivatePropertyName")

package de.menkalian.draco.data

object Draco {
    private const val BASE = "Draco"

    object Action {
        private const val BASE = Draco.BASE + ".Action"

        object Player {
            private const val BASE = Action.BASE + ".Player"
            override fun toString() = BASE

            const val Check = "$BASE.Check"
            const val Fold = "$BASE.Fold"
            const val Raise = "$BASE.Raise"
            const val Reveal = "$BASE.Reveal"
        }

        object Host {
            private const val BASE = Action.BASE + ".Host"
            override fun toString() = BASE

            const val CancelGame = "$BASE.CancelGame"
            const val StartGame = "$BASE.StartGame"
        }

        object Quizmaster {
            private const val BASE = Action.BASE + ".Quizmaster"
            override fun toString() = BASE

            const val Acknowledge = "$BASE.Acknowledge"
            const val Reveal = "$BASE.Reveal"
            const val RevealName = "$BASE.RevealName"
        }
    }

    object Connection {
        private const val BASE = Draco.BASE + ".Connection"

        object Server {
            private const val BASE = Connection.BASE + ".Server"

            object REST {
                private const val BASE = Server.BASE + ".REST"

                const val TLS = "$BASE.TLS"
                const val Host = "$BASE.Host"
                const val Port = "$BASE.Port"
            }

            object WS {
                private const val BASE = Server.BASE + ".WS"

                const val TLS = "$BASE.TLS"
                const val Host = "$BASE.Host"
                const val Port = "$BASE.Port"
                const val Path = "$BASE.Path"

                object Heartbeat {
                    private const val BASE = WS.BASE + ".Heartbeat"

                    const val Rate = "$BASE.Rate"
                    const val MaxMisses = "$BASE.MaxMisses"
                }
            }
        }
    }

    object Game {
        private const val BASE = Draco.BASE + ".Game"

        object Lobby {
            private const val BASE = Game.BASE + ".Lobby"

            const val Id = "$BASE.Id"
        }

        object Player {
            private const val BASE = Game.BASE + ".Player"

            const val Name = "$BASE.Name"
        }

        object Poker {
            private const val BASE = Game.BASE + ".Poker"

            const val State = "$BASE.State"
            const val Round = "$BASE.Round"
            const val CurrentPlayer = "$BASE.CurrentPlayer"
            const val CurrentBid = "$BASE.CurrentBid"

            object Question {
                private const val BASE = Poker.BASE + ".Question"

                const val UUID = "$BASE.UUID"
                const val Text = "$BASE.Text"
                const val Answer = "$BASE.Answer"

                object Hint {
                    private const val BASE = Question.BASE + ".Hint"

                    const val n = "$BASE.n"

                    fun XXX(idx: Int): XXXClass {
                        return XXXClass(idx)
                    }

                    class XXXClass(idx: Int) {
                        private val BASE = Hint.BASE + "." + idx.toString().padStart(3, '0')

                        val Text = "$BASE.Text"
                    }
                }
            }

            object Blinds {
                private const val BASE = Poker.BASE + ".Blinds"

                const val Small = "${BASE}.Small"
                const val Big = "${BASE}.Big"
            }

            object Settings {
                private const val BASE = Poker.BASE + ".Setting"
                override fun toString() = BASE

                object Lobby {
                    private const val BASE = Settings.BASE + ".Lobby"
                    const val Name = "$BASE.Name"
                    const val Publicity = "$BASE.Publicity"
                }

                const val DefaultPoints = "$BASE.DefaultPoints"
                const val Timeout = "$BASE.Timeout"
                const val MaxQuestions = "$BASE.MaxQuestions"

                const val KickBroke = "$BASE.KickBroke"
                const val ShowHelpWarnings = "$BASE.ShowHelpWarnings"
                const val LateJoin = "$BASE.LateJoin "

                const val BlindStrategy = "$BASE.BlindStrategy"
                const val RevealStrategy = "$BASE.RevealStrategy"
                const val TimeoutStrategy = "$BASE.TimeoutStrategy"
                const val LateJoinStrategy = "$BASE.LateJoinStrategy"

                object Categories {
                    private const val BASE = Settings.BASE + ".Categories"
                    const val n = "$BASE.n"

                    fun XXX(idx: Int): XXXClass {
                        return XXXClass(idx)
                    }

                    class XXXClass(idx: Int) {
                        private val BASE = Categories.BASE + "." + idx.toString().padStart(3, '0')

                        val Name = "$BASE.Text"
                    }
                }

                object Difficulties {
                    private const val BASE = Settings.BASE + ".Difficulties"
                    const val n = "$BASE.n"

                    fun XXX(idx: Int): XXXClass {
                        return XXXClass(idx)
                    }

                    class XXXClass(idx: Int) {
                        private val BASE = Difficulties.BASE + "." + idx.toString().padStart(3, '0')

                        val Name = "$BASE.Text"
                    }
                }

                object Languages {
                    private const val BASE = Settings.BASE + ".Languages"
                    const val n = "$BASE.n"

                    fun XXX(idx: Int): XXXClass {
                        return XXXClass(idx)
                    }

                    class XXXClass(idx: Int) {
                        private val BASE = Languages.BASE + "." + idx.toString().padStart(3, '0')

                        val Name = "$BASE.Text"
                    }
                }

                object Blinds {
                    private const val BASE = Settings.BASE + ".Blinds"
                    const val n = "$BASE.n"

                    fun XXX(idx: Int): XXXClass {
                        return XXXClass(idx)
                    }

                    class XXXClass(idx: Int) {
                        private val BASE = Blinds.BASE + "." + idx.toString().padStart(3, '0')

                        val Small = "$BASE.Small"
                        val Big = "$BASE.Big"
                    }
                }
            }

            object Winners {
                private const val BASE = Poker.BASE + ".Winners"

                const val n = "$BASE.n"
                const val Type = "$BASE.Type"

                fun XXX(idx: Int): XXXClass {
                    return XXXClass(idx)
                }

                class XXXClass(idx: Int) {
                    private val BASE = Winners.BASE + "." + idx.toString().padStart(3, '0')

                    val Name = "$BASE.Name"
                }
            }
        }
    }

    object Message {
        private const val BASE = Draco.BASE + ".Message"

        const val Id = "$BASE.Id"
        const val Timestamp = "$BASE.Timestamp"
        const val Type = "$BASE.Type"

        object Quizmaster {
            private const val BASE = Message.BASE + ".Quizmaster"

            object Stage {
                private const val BASE = Quizmaster.BASE + ".Stage"

                const val Current = "$BASE.Current"
                const val Next = "$BASE.Next"
            }
        }
    }

    object Player {
        private const val BASE = Draco.BASE + ".Player"
        override fun toString() = BASE

        object Poker {
            private const val BASE = Player.BASE + ".Poker"

            const val Role = "$BASE.Role"
            const val Score = "$BASE.Score"

            const val Answer = "$BASE.Answer"
            const val Revealed = "$BASE.Revealed"
            const val Folded = "$BASE.Folded"
            const val Pot = "$BASE.Pot"
        }

        object Connection {
            private const val BASE = Player.BASE + ".Connection"

            const val State = "$BASE.State"
            const val Ping = "$BASE.Ping"
        }
    }
}