package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class Microcontroller extends traits.PowerAcceptor with traits.Hub with traits.Computer with internal.Microcontroller {
  val info = new MicrocontrollerData()

  override def node = null

  val outputSides = Array.fill(6)(true)

  val snooperNode = api.Network.newNode(this, Visibility.Network).
    withComponent("microcontroller").
    withConnector(Settings.get.bufferMicrocontroller).
    create()

  val componentNodes = Array.fill(6)(api.Network.newNode(this, Visibility.Neighbors).
    withComponent("microcontroller").
    create())

  if (machine != null) {
    machine.node.asInstanceOf[Connector].setLocalBufferSize(0)
  }

  override def tier = info.tier

  override protected def runSound = None // Microcontrollers are silent.

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != facing

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing) snooperNode else null)

  override protected def energyThroughput = Settings.get.caseRate(Tier.One)

  override def getWorld = world

  // ----------------------------------------------------------------------- //

  override def internalComponents(): java.lang.Iterable[ItemStack] = asJavaIterable(info.components)

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():boolean -- Starts the microcontroller. Returns true if the state changed.""")
  def start(context: Context, args: Arguments): Array[AnyRef] =
    result(!machine.isPaused && machine.start())

  @Callback(doc = """function():boolean -- Stops the microcontroller. Returns true if the state changed.""")
  def stop(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.stop())

  @Callback(direct = true, doc = """function():boolean -- Returns whether the microcontroller is running.""")
  def isRunning(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.isRunning)

  @Callback(direct = true, doc = """function():string -- Returns the reason the microcontroller crashed, if applicable.""")
  def lastError(context: Context, args: Arguments): Array[AnyRef] =
    result(machine.lastError)

  @Callback(direct = true, doc = """function(side:number):boolean -- Get whether network messages are sent via the specified side.""")
  def isSideOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS.filter(_ != facing): _*)
    result(outputSides(side.ordinal()))
  }

  @Callback(doc = """function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.""")
  def setSideOpen(context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS.filter(_ != facing): _*)
    val oldValue = outputSides(side.ordinal())
    outputSides(side.ordinal()) = args.checkBoolean(1)
    result(oldValue)
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()

    // Pump energy into the internal network.
    if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if side != facing) {
        sidedNode(side) match {
          case connector: Connector =>
            val demand = snooperNode.globalBufferSize - snooperNode.globalBuffer
            val available = demand + connector.changeBuffer(-demand)
            snooperNode.changeBuffer(available)
          case _ =>
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def connectItemNode(node: Node) {
    if (machine.node != null && node != null) {
      machine.node.connect(node)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).
    withConnector().
    create()

  override protected def onPlugConnect(plug: Plug, node: Node): Unit = {
    if (node == plug.node) {
      api.Network.joinNewNetwork(machine.node)
      machine.node.connect(snooperNode)
      machine.setCostPerTick(Settings.get.microcontrollerCost)
      node.connect(componentNodes(plug.side.ordinal))
    }
    super.onPlugConnect(plug, node)
  }

  override protected def onPlugMessage(plug: Plug, message: Message): Unit = {
    if (message.name == "network.message" && message.source.network != snooperNode.network) {
      snooperNode.sendToReachable(message.name, message.data: _*)
    }
  }

  override def onMessage(message: Message): Unit = {
    if (message.source.network == snooperNode.network) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if outputSides(side.ordinal)) {
        sidedNode(side).sendToReachable(message.name, message.data: _*)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    // Load info before inventory and such, to avoid initializing components
    // to empty inventory.
    info.load(nbt.getCompoundTag(Settings.namespace + "info"))
    nbt.getBooleanArray(Settings.namespace + "outputs")
    super.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewCompoundTag(Settings.namespace + "info", info.save)
    nbt.setBooleanArray(Settings.namespace + "outputs", outputSides)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    info.load(nbt.getCompoundTag("info"))
    super.readFromNBTForClient(nbt)
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag("info", info.save)
  }

  override lazy val items = info.components.map(Option(_))

  override def getSizeInventory = info.components.length

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = false

  // Nope.
  override def setInventorySlotContents(slot: Int, stack: ItemStack) {}

  // Nope.
  override def decrStackSize(slot: Int, amount: Int) = null
}
